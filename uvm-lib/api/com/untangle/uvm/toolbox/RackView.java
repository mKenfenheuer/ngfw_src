/*
 * $HeadURL: svn://chef/branch/prod/web-ui/work/src/uvm-lib/api/com/untangle/uvm/node/NodeManager.java $
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module.  An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your version
 * of the library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */

package com.untangle.uvm.toolbox;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.untangle.uvm.license.LicenseStatus;
import com.untangle.uvm.message.StatDescs;
import com.untangle.uvm.node.NodeDesc;
import com.untangle.uvm.node.NodeState;
import com.untangle.uvm.security.NodeId;

@SuppressWarnings("serial")
public class RackView implements Serializable
{
    private List<Application> applications;
    private List<NodeDesc> instances;
    private Map<NodeId, StatDescs> statDescs;
    private Map<String, LicenseStatus> licenseStatus;
    private Map<NodeId, NodeState> runStates;

    public RackView(List<Application> applications, List<NodeDesc> instances,
                    Map<NodeId, StatDescs> statDescs,
                    Map<String, LicenseStatus> licenseStatus,
                    Map<NodeId, NodeState> runStates)
    {
        this.applications = Collections.unmodifiableList(applications);
        this.instances = Collections.unmodifiableList(instances);
        this.statDescs = Collections.unmodifiableMap(statDescs);
        this.licenseStatus = Collections.unmodifiableMap(licenseStatus);
        this.runStates = Collections.unmodifiableMap(runStates);
    }

    public List<Application> getApplications()
    {
        return applications;
    }

    public List<NodeDesc> getInstances()
    {
        return instances;
    }

    public Map<NodeId, StatDescs> getStatDescs()
    {
        return statDescs;
    }

    public Map<String, LicenseStatus> getLicenseStatus()
    {
        return licenseStatus;
    }

    public Map<NodeId, NodeState> getRunStates() {
        return runStates;
    }

    @Override
    public String toString()
    {
        return "RackView\n  AVAILABLE: " + applications + "\n  INSTANCES: " + instances + "\n  STAT DESCS: " + statDescs;
    }

}