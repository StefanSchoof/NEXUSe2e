/**
 * NEXUSe2e Business Messaging Open Source  
 * Copyright 2007, Tamgroup and X-ioma GmbH   
 *  
 * This is free software; you can redistribute it and/or modify it  
 * under the terms of the GNU Lesser General Public License as  
 * published by the Free Software Foundation version 2.1 of  
 * the License.  
 *  
 * This software is distributed in the hope that it will be useful,  
 * but WITHOUT ANY WARRANTY; without even the implied warranty of  
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU  
 * Lesser General Public License for more details.  
 *  
 * You should have received a copy of the GNU Lesser General Public  
 * License along with this software; if not, write to the Free  
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.nexuse2e;

/**
 * Interface for entities that are specific to an action within a choreogrpahy.
 *
 * @author gesch
 */
public interface ActionSpecific {

    /**
     * @param key The key identifying the combination of an action and a choreography.
     */
    public void setKey( ActionSpecificKey key );

    /**
     * 
     * @return The key identifying the combination of an action and a choreography.
     */
    public ActionSpecificKey getKey();
} // ActionSpecific
