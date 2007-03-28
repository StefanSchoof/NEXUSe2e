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
package org.nexuse2e.ui.action.serverconfig;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessages;
import org.nexuse2e.Engine;
import org.nexuse2e.pojo.LoggerPojo;
import org.nexuse2e.ui.action.NexusE2EAction;
import org.nexuse2e.ui.form.LoggerForm;

/**
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 *
 * @author markus.breilmann
 */
public class NotifierListAction extends NexusE2EAction {

    //private static final String VERSIONSTRING = "$Id: NotifierListAction.java 879 2005-07-21 14:17:36Z markus.breilmann $";

    /* (non-Javadoc)
     * @see com.tamgroup.nexus.e2e.ui.action.NexusE2EAction#executeNexusE2EAction(org.apache.struts.action.ActionMapping, org.apache.struts.action.ActionForm, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, org.apache.struts.action.ActionMessages)
     */
    @Override
    public ActionForward executeNexusE2EAction( ActionMapping actionMapping, ActionForm actionForm,
            HttpServletRequest request, HttpServletResponse response, ActionMessages errors, ActionMessages messages )
            throws Exception {

        ActionForward actionForward = actionMapping.findForward( ACTION_FORWARD_SUCCESS );
        List<LoggerPojo> loggers = null;

        LoggerForm loggerForm = (LoggerForm) actionForm;
        int nxChoreographyId = loggerForm.getNxChoreographyId();
        if ( nxChoreographyId != 0 ) {
            // loggers = Engine.getInstance().getConfigAccessService().getLoggersByNxChoreographyId();
        } else {
            loggers = Engine.getInstance().getActiveConfigurationAccessService().getLoggers();
        }

        List<LoggerForm> LoggerList = new ArrayList<LoggerForm>();
        for ( LoggerPojo logger : loggers ) {
            LoggerForm tempNotifierForm = new LoggerForm();
            tempNotifierForm.setProperties( logger );
            LoggerList.add( tempNotifierForm );
        }
        request.setAttribute( ATTRIBUTE_COLLECTION, LoggerList );

        if ( !errors.isEmpty() ) {
            actionForward = actionMapping.findForward( ACTION_FORWARD_FAILURE );
        }

        return actionForward;
    } // executeNexusE2EAction

} // NotifierListAction
