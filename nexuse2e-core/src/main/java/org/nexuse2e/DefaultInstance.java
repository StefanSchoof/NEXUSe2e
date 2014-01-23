/**
 *  NEXUSe2e Business Messaging Open Source
 *  Copyright 2000-2009, Tamgroup and X-ioma GmbH
 *
 *  This is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU Lesser General Public License as
 *  published by the Free Software Foundation version 2.1 of
 *  the License.
 *
 *  This software is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this software; if not, write to the Free
 *  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.nexuse2e;

import java.util.ArrayList;
import java.util.List;

public class DefaultInstance implements InstanceInterface {

	@Override
	public List<InstanceCommand> getCommands() {
		List<InstanceCommand> commands = new ArrayList<InstanceCommand>();
		commands.add(new InstanceCommand("Stop Engine", "stop"));
		commands.add(new InstanceCommand("Start Engine", "start"));
		commands.add(new InstanceCommand("Restart Engine", "restart"));
		return commands;
	}

	/* (non-Javadoc)
	 * @see org.nexuse2e.InstanceInterface#getLabel()
	 */
	@Override
	public String getLabel() {
		return "Local Instance";
	}

	/* (non-Javadoc)
	 * @see org.nexuse2e.InstanceInterface#getId()
	 */
	@Override
	public String getId() {
		
		return "local";
	}

	/* (non-Javadoc)
	 * @see org.nexuse2e.InstanceInterface#getStatus()
	 */
	@Override
	public String getStatus() {
		try {
			EngineStatusSummary summary = Engine.getInstance().getEngineController().getEngineMonitor().getStatus();
			return summary.getStatus().name();
		} catch (NexusException e) {
			return "error";
		}
	}

	/* (non-Javadoc)
	 * @see org.nexuse2e.InstanceInterface#getStatusColor()
	 */
	@Override
	public Color getStatusColor() {
		try {
			EngineStatusSummary summary = Engine.getInstance().getEngineController().getEngineMonitor().getStatus();
			switch(summary.getStatus()) {
			case ACTIVE:
				return Color.GREEN;
			case ERROR:
				return Color.RED;
			case INACTIVE:
				return Color.YELLOW;
			case UNKNOWN:
				return Color.GREY;
			default:
				return Color.RED;
			}
		} catch (NexusException e) {
			return Color.RED;
		}
	}

	/* (non-Javadoc)
	 * @see org.nexuse2e.InstanceInterface#executeCommand(java.lang.String)
	 */
	@Override
	public void executeCommand(String commandId) {
		if("restart".equals(commandId)) {
			new Thread() {
	            public void run() {
	                try {
	                    Engine.getInstance().changeStatus( BeanStatus.INSTANTIATED );
	                    Engine.getInstance().changeStatus( BeanStatus.STARTED );
	                } catch (InstantiationException e) {
	                }
	            }
	        }.start();
		}
		if("stop".equals(commandId)) {
			new Thread() {
	            public void run() {
	                try {
	                    Engine.getInstance().changeStatus( BeanStatus.INSTANTIATED );
	                } catch (InstantiationException e) {
	                }
	            }
	        }.start();
		}
		if("start".equals(commandId)) {
			new Thread() {
	            public void run() {
	                try {
	                    Engine.getInstance().changeStatus( BeanStatus.STARTED );
	                } catch (InstantiationException e) {
	                }
	            }
	        }.start();
		}
	}

}
