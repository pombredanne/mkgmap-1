/*
 * Copyright (C) 2007 Steve Ratcliffe
 * 
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License version 2 as
 *  published by the Free Software Foundation.
 * 
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 * 
 * Author: Steve Ratcliffe
 * Create date: Jan 5, 2008
 */
package uk.me.parabola.imgfmt.app.net;

import java.util.ArrayList;
import java.util.List;

import uk.me.parabola.imgfmt.app.ImgFileWriter;
import uk.me.parabola.imgfmt.app.IntList;
import uk.me.parabola.imgfmt.app.Label;
import uk.me.parabola.imgfmt.app.OffsetWriterList;
import uk.me.parabola.imgfmt.app.trergn.Polyline;
import uk.me.parabola.log.Logger;

/**
 * A road definition.  This ties together all segments of a single road
 * and provides street address information.
 *
 * This corresponds to an entry in NET1, which is linked with the
 * polylines making up this road in RGN. Links to RGN are written
 * via RoadIndex, while writing links from RGN to NET1 is delayed
 * via setOffsetWriter.
 *
 * If the map includes routing, the NET1 record also points to
 * a NOD2 record, written by writeNod2.
 *
 * Edges in the routing graph ("arcs") link to the corresponding
 * road via the RoadDef, storing the NET1 offset via TableA,
 * which also includes some road information.
 *
 * @author Elrond
 * @author Steve Ratcliffe
 * @author Robert Vollmert
 */

public class RoadDef {
	private static final Logger log = Logger.getLogger(RoadDef.class);

	// the offset in Nod2 of our Nod2 record
	private int offsetNod2;

	// the offset in Net1 of our Net1 record
	private int offsetNet1;

	// for diagnostic purposes
	private long roadId;
	private String name;

	public RoadDef(long roadId, String name) {
		this.roadId = roadId;
		this.name = name;
	}

	// for diagnostic purposes
	public String toString() {
		return "RoadDef(" + name + ", " + roadId + ")";
	}

	/*
	 * Everything that's relevant for writing to NET1.
	 */

	private static final byte FLAG_UNK1 = 0x04;
	private static final byte FLAG_HAS_NOD_INFO = 0x40;

	private byte netFlags = FLAG_UNK1;

	// The road length units may be affected by other flags in the header as
	// there is doubt as to the formula.
	private int roadLength = 300; // XXX set the road length

	// There can be up to 4 labels for the same road.
	private static final int MAX_LABELS = 4;

	private final Label[] labels = new Label[MAX_LABELS];
	private int numlabels;

	// this may stay
	@Deprecated private List<RoadIndex> roadIndexes = new ArrayList<RoadIndex>();

	/**
	 * This is for writing to NET1.
	 * @param writer A writer that is positioned within NET1.
	 */
	void writeNet1(ImgFileWriter writer) {
		if (numlabels == 0)
			return;
		assert numlabels > 0;

		offsetNet1 = writer.position();

		writeLabels(writer);
		writer.put(netFlags);
		writer.put3(roadLength);

		int maxlevel = writeLevelCount(writer);

		writeLevelDivs(writer, maxlevel);

		if (hasNodInfo()) {
			// This is the offset of an entry in NOD2
			int val = offsetNod2;
			if (val < 0x7fff) {
				writer.put((byte) 1);
				writer.putChar((char) val);
			} else {
				writer.put((byte) 2);
				writer.put3(val);
			}
		}
	}

	private void writeLabels(ImgFileWriter writer) {
		for (int i = 0; i < numlabels; i++) {
			Label l = labels[i];
			int ptr = l.getOffset();
			if (i == (numlabels-1))
				ptr |= 0x800000;
			writer.put3(ptr);
		}
	}

	private int writeLevelCount(ImgFileWriter writer) {
		int maxlevel = getMaxZoomLevel();
		for (int i = 0; i <= maxlevel; i++) {
			byte b = 0;
			for (RoadIndex ri : roadIndexes) {
				if (ri.getZoomLevel() == i)
					b++;
			}
			if (i == maxlevel)
				b |= 0x80;
			writer.put(b);
		}
		return maxlevel;
	}

	private void writeLevelDivs(ImgFileWriter writer, int maxlevel) {
		for (int i = 0; i <= maxlevel; i++) {
			for (RoadIndex ri : roadIndexes) {
				if (ri.getZoomLevel() == i)
					ri.write(writer);
			}
		}
	}

	public void addLabel(Label l) {
		// XXX: apparently, just one label for now?
		if (numlabels == 0)
			labels[numlabels++] = l;
		else
			log.warn("discarding extra label", l);
	}

	public void addPolylineRef(Polyline pl) {
		roadIndexes.add(new RoadIndex(pl));
	}

	private int getMaxZoomLevel() {
		int m = 0;
		for (RoadIndex ri : roadIndexes) {
			int z = ri.getZoomLevel();
			m = (z > m ? z : m);
		}
		return m;
	}

	/*
	 * Everything that's relevant for writing to RGN.
	 */

	private IntList rgnOffsets = new IntList(4);

	/**
	 * Add a target location in the RGN section where we should write the
	 * offset of this road def when it is written to NET.
	 */
	public void addOffsetTarget(ImgFileWriter writer, int ormask) {
		rgnOffsets.add(writer.position() | ormask);
	}

	/**
	 * Write into the RGN the offset in net1 of this road.
	 * @param rgn A writer for the rgn file.
	 */
	void writeRgnOffsets(ImgFileWriter rgn) {
		int len = rgnOffsets.size();
		for (int i = 0; i < len; i++) {
			int off = rgnOffsets.get(i);
			rgn.position(off & 0x3fffff);
			rgn.put3(offsetNet1 | (off & 0xc00000));
		}
	}

	/*
	 * Everything that's relevant for writing out Nod 2.
	 */

	// This is the node associated with the road.  I'm not certain about how
	// this works, but in NOD2 each road has a reference to only one node.
	// This is that node.
	private RouteNode node;

	// XXX: hard-coded for now
	private byte roadClass = (byte) 0x4d;

	/**
	 * Set the routing node associated with this road.
	 *
	 * This implies that the road has an entry in NOD 2
	 * which will be pointed at from NET 1.
	 */
	public void setNode(RouteNode node) {
		netFlags |= FLAG_HAS_NOD_INFO;
		this.node = node;
	}

	private boolean hasNodInfo() {
		return (netFlags & FLAG_HAS_NOD_INFO) != 0;
	}

	/**
	 * Write this road's NOD2 entry.
	 *
	 * Assumes that setNode(RoutingNode) has been called before.
	 * Stores the writing position to be able to link here
	 * from NET 1 later.
	 *
	 * @param writer A writer positioned in NOD2.
	 */
	public void writeNod2(ImgFileWriter writer) {
		log.debug("writing nod2");

		assert hasNodInfo() : "writing Nod2 without NodInfo";

		offsetNod2 = writer.position();

		writer.put(roadClass);
		writer.put3(node.getOffsetNod1()); // offset in nod1

		// this is related to the number of nodes, but there is more to it...
		// XXX: hard-coded to 3 for now
		char nnodes = (char) 3;
		writer.putChar(nnodes);  // number of bits to follow
		writer.put((byte) ((1<<nnodes)-1));
	}

	/*
	 * Everything that's relevant for writing out Table A.
	 *
	 * Storing this info in the RoadDef means that each
	 * arc gets the same version of the below info, which
	 * makes sense for the moment considering polish format
	 * doesn't provide for different speeds and restrictions
	 * for segments of roads.
	 */

	/**
	 * Return the offset of this road's NET1 entry. Assumes
	 * writeNet1() has been called.
	 */
	public int getOffsetNet1() {
		return offsetNet1;
	}

	// first byte of Table A info in NOD 1
	private static final int INFO_TOLL = 0x80;
	private static final int INFO_CLASS = 0x70;
	private static final int INFO_ONEWAY = 0x08;
	private static final int INFO_SPEED = 0x07;
	// second byte: restrictions
	private static final int NO_BIKE = 0x20;
	private static final int NO_FOOT = 0x10;
	// missing restrictions: emergency, delivery, bus, car, taxi, truck

	// The data for Table A
	// XXX: hard-coded for now
	private byte tabAInfo = 0x46; // class 4, speed 6
	private byte tabARestrictions = 0x00; // no restrictions

	public byte getTabAInfo() {
		return tabAInfo;
	}

	public byte getTabARestrictions() {
		return tabARestrictions;
	}
}
