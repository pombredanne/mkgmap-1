package uk.me.parabola.mkgmap.reader.osm;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 
 * Represent a Relation.
 * 
 * @author Rene_A
 */
public abstract class Relation extends Element {
	private final Map<Element, String> roles = new LinkedHashMap<Element, String>();
	private final List<Element> elements = new ArrayList<Element>();

	/** 
	 * Add an Element, role pair to this Relation.
	 * FIXME: Only one role can be associated to an element
	 * @param role The role this element performs in this relation
	 * @param el The Element added
	 */
	public void addElement(String role, Element el) {
		roles.put(el, role);
		elements.add(el);
	}

	public abstract void processElements();

	public List<Element> getElements() {
		return elements;
	}

	public Map<Element, String> getRoles() {
		return roles;
	}

	public String kind() {
		return "relation";
	}
}
