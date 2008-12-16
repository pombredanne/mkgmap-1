/*
 * Copyright (C) 2008 Steve Ratcliffe
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
 * Create date: 06-Dec-2008
 */
package uk.me.parabola.mkgmap.osmstyle.actions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import uk.me.parabola.mkgmap.reader.osm.Element;
import uk.me.parabola.mkgmap.reader.osm.Relation;

/**
 * This is an action that contains sub-actions.  It is used for Relations
 * where you want to apply the commands to the elements that are contained
 * in the relation and not on the relation itself.
 *
 * @author Steve Ratcliffe
 */
public class SubAction implements Action {
	private final List<Action> actionList = new ArrayList<Action>();
	private final String role;

	public SubAction(String role) {
		this.role = role;
	}

	public void perform(Element el) {
		if (el instanceof Relation)
			performOnSubElements((Relation) el);
	}

	private void performOnSubElements(Relation rel) {
		List<Element> elements = rel.getElements();
		Map<Element, String> roles = rel.getRoles();
		for (Element el : elements) {
			if (role == null || role.equals(roles.get(el))) {
				for (Action a : actionList) {
					if (a instanceof AddTagAction)
						((AddTagAction) a).setValueTags(rel);
					a.perform(el);
				}
			}
		}
	}

	public void add(Action act) {
		actionList.add(act);
	}
}