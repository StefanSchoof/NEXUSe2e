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
package org.nexuse2e.ui.action;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessages;
import org.nexuse2e.Engine;
import org.nexuse2e.pojo.ServicePojo;
import org.nexuse2e.ui.form.ServiceForm;

/**
 * This action provides data required to list all services (and their properties).
 * @author jonas.reese
 */
public class ServicesAction extends NexusE2EAction {

    @Override
    public ActionForward executeNexusE2EAction( ActionMapping actionMapping, ActionForm actionForm,
            HttpServletRequest request, HttpServletResponse response, ActionMessages errors, ActionMessages messages )
            throws Exception {

        ActionForward actionForward = actionMapping.findForward( ACTION_FORWARD_SUCCESS );

        List<ServicePojo> services = Engine.getInstance().getActiveConfigurationAccessService().getServices();
        List<ServiceForm> serviceList = new ArrayList<ServiceForm>();
        for ( ServicePojo service : services ) {
            ServiceForm serviceForm = new ServiceForm();
            serviceForm.setProperties( service );
            serviceForm.setServiceInstance( Engine.getInstance().getActiveConfigurationAccessService()
                    .getService( service.getName() ) );
            serviceList.add( serviceForm );
        }
        request.setAttribute( ATTRIBUTE_COLLECTION, serviceList );

        if ( !errors.isEmpty() ) {
            actionForward = actionMapping.findForward( ACTION_FORWARD_FAILURE );
        }

        return actionForward;
    } // executeNexusE2EAction

}
