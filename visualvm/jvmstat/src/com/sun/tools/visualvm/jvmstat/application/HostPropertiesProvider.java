/*
 * Copyright 2007-2008 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * 
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
 * 
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 * 
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 * 
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

package com.sun.tools.visualvm.jvmstat.application;

import com.sun.tools.visualvm.core.datasource.Storage;
import com.sun.tools.visualvm.core.properties.PropertiesPanel;
import com.sun.tools.visualvm.core.properties.PropertiesProvider;
import com.sun.tools.visualvm.host.Host;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 *
 * @author Jiri Sedlacek
 */
public class HostPropertiesProvider extends PropertiesProvider<Host> {

    private static final String PROP_JSTATD_PORT = "prop_jstatd_port"; // NOI18N
    private static final String PROP_JSTATD_REFRESH = "prop_jstatd_refresh"; // NOI18N


    public HostPropertiesProvider() {
        super("jstatd", "Configures jvmstat connections to the Host", 10);
    }


    public boolean supportsDataSource(Host host) {
        return true;
    }

    public PropertiesPanel createPanel(Host host) {
        return new ConnectionsCustomizer(getDescriptorsEx(host));
    }

    public void propertiesDefined(PropertiesPanel panel, Host host) {
        ConnectionsCustomizer customizer = (ConnectionsCustomizer)panel;
        setDescriptors(host, customizer.getDescriptors());
    }

    public void propertiesChanged(PropertiesPanel panel, Host host) {
        ConnectionsCustomizer customizer = (ConnectionsCustomizer)panel;
        setDescriptorsEx(host, customizer.getDescriptors());
    }

    public void propertiesCancelled(PropertiesPanel panel, Host host) {
        // Nothing to do
    }

    public static void initializeLocalhost() {
        Host host = Host.LOCALHOST;
        setDescriptors(host, getDescriptorsEx(null)); // TODO: handle customizations!
    }

    static Set<ConnectionDescriptor> getDescriptors(Host host) {
        Set<ConnectionDescriptor> set = new HashSet();

        if (host != null) {
            Storage storage = host.getStorage();
            int index = 0;
            String port = storage.getCustomProperty(PROP_JSTATD_PORT + "." + index); // NOI18N
            while (port != null) {
                String refresh = storage.getCustomProperty(PROP_JSTATD_REFRESH + "." + index); // NOI18N
                try {
                    set.add(new ConnectionDescriptor(Integer.parseInt(port), Double.parseDouble(refresh)));
                } catch (NumberFormatException e) {
                    // TODO: log it
                }
                port = storage.getCustomProperty(PROP_JSTATD_PORT + "." + ++index); // NOI18N
            }
        }

        return set;
    }

    private static Set<ConnectionDescriptor> getDescriptorsEx(Host host) {
        Set<ConnectionDescriptor> set = getDescriptors(host);
        if (host == null) set.add(ConnectionDescriptor.createDefault());
        return set;
    }

    private static void setDescriptors(Host host, Set<ConnectionDescriptor> descriptors) {
        Storage storage = host.getStorage();
        clearDescriptors(storage);
        int index = 0;
        for (ConnectionDescriptor descriptor : descriptors) {
            storage.setCustomProperty(PROP_JSTATD_PORT + "." + index, // NOI18N
                    Integer.toString(descriptor.getPort()));
            storage.setCustomProperty(PROP_JSTATD_REFRESH + "." + index, // NOI18N
                    Double.toString(descriptor.getRefreshRate()));
            index++;
        }
    }

    private static void setDescriptorsEx(Host host, Set<ConnectionDescriptor> newDescriptors) {
        // Cache old descriptors
        List<ConnectionDescriptor> oldDescriptors = new ArrayList(getDescriptorsEx(host));

        // Set new descriptors
        setDescriptors(host, newDescriptors);

        // Resolve added descriptors
        Set<ConnectionDescriptor> added = new HashSet(newDescriptors);
        added.removeAll(oldDescriptors);

        // Resolve removed descriptors
        Set<ConnectionDescriptor> removed = new HashSet(oldDescriptors);
        removed.removeAll(newDescriptors);

        // Resolve changed descriptors
        Set<ConnectionDescriptor> changed = new HashSet(newDescriptors);
        changed.retainAll(oldDescriptors);
        Iterator<ConnectionDescriptor> iterator = changed.iterator();
        while (iterator.hasNext()) {
            ConnectionDescriptor descriptor1 = iterator.next();
            ConnectionDescriptor descriptor2 = oldDescriptors.get(oldDescriptors.indexOf(descriptor1));

            if (Math.abs(descriptor1.getRefreshRate() - descriptor2.getRefreshRate()) < 0.001) {
                iterator.remove();
            }
        }

//        System.err.println(">>> added:   " + added);
//        System.err.println(">>> removed: " + removed);
//        System.err.println(">>> changed: " + changed);

        // TODO: implement JvmstatApplicationProvider.connectionsChanged:
//        if (!added.isEmpty() || !removed.isEmpty() || !changed.isEmpty())
//            JvmstatApplicationProvider.sharedInstance().connectionsChanged(
//                    host, added, removed, changed);
    }

    private static void clearDescriptors(Storage storage) {
        int index = 0;
        String port = storage.getCustomProperty(PROP_JSTATD_PORT + "." + index); // NOI18N
        while (port != null) {
            storage.clearCustomProperties(new String[] {
                PROP_JSTATD_PORT + "." + index, PROP_JSTATD_REFRESH + "." + index // NOI18N
            });
            port = storage.getCustomProperty(PROP_JSTATD_PORT + "." + ++index); // NOI18N
        }
    }

}
