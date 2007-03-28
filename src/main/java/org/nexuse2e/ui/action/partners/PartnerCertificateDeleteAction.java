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
package org.nexuse2e.ui.action.partners;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;
import org.nexuse2e.Engine;
import org.nexuse2e.NexusException;
import org.nexuse2e.configuration.Constants;
import org.nexuse2e.pojo.CertificatePojo;
import org.nexuse2e.pojo.PartnerPojo;
import org.nexuse2e.ui.action.NexusE2EAction;
import org.nexuse2e.ui.form.PartnerCertificateForm;

/**
 * @author gesch
 *
 */
public class PartnerCertificateDeleteAction extends NexusE2EAction {

    private static final String VERSIONSTRING = "$Id: PartnerCertificateDeleteAction.java 925 2005-08-02 16:50:24Z guido.esch $";

    private static String       URL           = "partner.error.url";
    private static String       TIMEOUT       = "partner.error.timeout";

    /* (non-Javadoc)
     * @see com.tamgroup.nexus.e2e.ui.action.NexusE2EAction#executeNexusE2EAction(org.apache.struts.action.ActionMapping, org.apache.struts.action.ActionForm, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, org.apache.struts.action.ActionMessages)
     */
    @Override
    public ActionForward executeNexusE2EAction( ActionMapping actionMapping, ActionForm actionForm,
            HttpServletRequest request, HttpServletResponse response, ActionMessages errors, ActionMessages messages )
            throws Exception {

        ActionForward success = actionMapping.findForward( ACTION_FORWARD_SUCCESS );
        ActionForward error = actionMapping.findForward( ACTION_FORWARD_SUCCESS );

        PartnerCertificateForm form = (PartnerCertificateForm) actionForm;

        int nxPartnerId = form.getNxPartnerId();
        int nxCertificateId = form.getNxCertificateId();

        if ( nxPartnerId == 0 ) {
            ActionMessage errorMessage = new ActionMessage( "generic.error", "Invalid nxPartnerId:" + nxPartnerId );
            errors.add( ActionMessages.GLOBAL_MESSAGE, errorMessage );
            addRedirect( request, URL, TIMEOUT );
            return error;
        }
        if ( nxCertificateId < 1 ) {
            ActionMessage errorMessage = new ActionMessage( "generic.error", "Invalid nxCertificateId:"
                    + nxCertificateId );
            errors.add( ActionMessages.GLOBAL_MESSAGE, errorMessage );
            addRedirect( request, URL, TIMEOUT );
            return error;
        }

        try {
            PartnerPojo partner = Engine.getInstance().getActiveConfigurationAccessService().getPartnerByNxPartnerId( nxPartnerId );
            CertificatePojo certificate = Engine.getInstance().getActiveConfigurationAccessService()
                    .getCertificateFromPartnerByNxCertificateId( partner, nxCertificateId );
            partner.getCertificates().remove( certificate );
            certificate.setPartner( null );
            if ( certificate != null ) {
                Engine.getInstance().getActiveConfigurationAccessService().deleteCertificate( Constants.CERTIFICATE_TYPE_ALL,
                        certificate );
            }
        } catch ( NexusException e ) {
            ActionMessage errorMessage = new ActionMessage( "generic.error", e.getMessage() );
            errors.add( ActionMessages.GLOBAL_MESSAGE, errorMessage );
            addRedirect( request, URL, TIMEOUT );
            return error;
        }
        return success;
    }

}
