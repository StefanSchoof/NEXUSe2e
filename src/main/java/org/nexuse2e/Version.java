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

public class Version {

    public static final String  distributor        = "X�ioma GmbH";
    // private static final String VERSIONSTRING      = "$Id: Version.java.template 1287 2006-07-24 10:20:36Z markus.breilmann $";
    private static final String subversionRevision = "2016";
    private static final String subversionDate     = "2007/03/09 16:00:00";
    private static final String version            = "4.0.0 beta5, Build " + subversionRevision + " ("
                                                           + subversionDate + ")";

    public static String getVersion() {

        return version;
    }

    public static void main( String[] args ) {
		System.out.println( "NexusE2E Version: " + version );
    }

} // Version