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
package org.nexuse2e.pojo;

// Generated 02.11.2006 11:50:54 by Hibernate Tools 3.2.0.beta6a

import java.util.Date;

/**
 * ComponentPojo generated by hbm2java
 */
public class ComponentPojo implements java.io.Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 4673827674202399344L;

    // Fields    
    private int               nxComponentId;
    private Date              createdDate;
    private Date              modifiedDate;
    private int               modifiedNxUserId;
    private int               type;
    private String            name;
    private String            className;
    private String            description;

    // Constructors

    /** default constructor */
    public ComponentPojo() {
        createdDate = new Date();
        modifiedDate = createdDate;
    }

    /** minimal constructor */
    public ComponentPojo( Date createdDate, Date modifiedDate, int modifiedNxUserId, int type, String name,
            String className ) {

        this.createdDate = createdDate;
        this.modifiedDate = modifiedDate;
        this.modifiedNxUserId = modifiedNxUserId;
        this.type = type;
        this.name = name;
        this.className = className;
    }

    /** full constructor */
    public ComponentPojo( Date createdDate, Date modifiedDate, int modifiedNxUserId, int type, String name,
            String className, String description ) {

        this.createdDate = createdDate;
        this.modifiedDate = modifiedDate;
        this.modifiedNxUserId = modifiedNxUserId;
        this.type = type;
        this.name = name;
        this.className = className;
        this.description = description;
    }

    // Property accessors
    public int getNxComponentId() {

        return this.nxComponentId;
    }

    public void setNxComponentId( int nxComponentId ) {

        this.nxComponentId = nxComponentId;
    }

    public Date getCreatedDate() {

        return this.createdDate;
    }

    public void setCreatedDate( Date createdDate ) {

        this.createdDate = createdDate;
    }

    public Date getModifiedDate() {

        return this.modifiedDate;
    }

    public void setModifiedDate( Date modifiedDate ) {

        this.modifiedDate = modifiedDate;
    }

    public int getModifiedNxUserId() {

        return this.modifiedNxUserId;
    }

    public void setModifiedNxUserId( int modifiedNxUserId ) {

        this.modifiedNxUserId = modifiedNxUserId;
    }

    public int getType() {

        return this.type;
    }

    public void setType( int type ) {

        this.type = type;
    }

    public String getName() {

        return this.name;
    }

    public void setName( String name ) {

        this.name = name;
    }

    public String getClassName() {

        return this.className;
    }

    public void setClassName( String className ) {

        this.className = className;
    }

    public String getDescription() {

        return this.description;
    }

    public void setDescription( String description ) {

        this.description = description;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals( Object obj ) {

        if ( obj instanceof ComponentPojo ) {
            if ( nxComponentId == 0 ) {
                return super.equals( obj );
            }
            return ( nxComponentId == ( (ComponentPojo) obj ).getNxComponentId() );
        }
        return false;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {

        if ( nxComponentId == 0 ) {
            return super.hashCode();
        }

        return nxComponentId;
    }

}
