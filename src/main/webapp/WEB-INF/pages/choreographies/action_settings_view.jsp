<%@ taglib uri="/tags/nexus" prefix="nexus"%>
<%@ taglib uri="/tags/struts-bean" prefix="bean"%>
<%@ taglib uri="/tags/struts-html" prefix="html"%>
<%@ taglib uri="/tags/struts-nested" prefix="nested"%>
<%@ taglib uri="/tags/struts-tiles" prefix="tiles"%>
<%@ taglib uri="/tags/struts-logic" prefix="logic"%>
<%@ taglib uri="http://java.sun.com/jstl/core" prefix="c"%>

<%@ taglib uri="/tags/struts-html-el" prefix="html-el"%>

<nexus:helpBar helpDoc="documentation/Action.htm"/>

<html:form
	action="ActionSettingsUpdate">
	<html:hidden property="nxChoreographyId" name="choreographyActionForm" />
	<html:hidden property="nxActionId" name="choreographyActionForm" />
	
	<table class="NEXUS_TABLE" width="100%">
    <tr>
        <td>
        	<nexus:crumbs/>
        </td>
    </tr>
    <tr>
        <td class="NEXUSScreenName">Update Action &gt; <bean:write name="choreographyActionForm" property="actionId" /></td>
    </tr>
	</table>
	
	<table class="NEXUS_TABLE" WIDTH="100%">
		<tr>
			<td class="NEXUSSection" colspan="2">Action Parameters</td>
		</tr>

		<tr>
			<td class="NEXUSValue" colspan="2"><html:checkbox
				property="startAction">Valid Start Action</html:checkbox></td>
		</tr>
		<tr>
			<td class="NEXUSValue" colspan="2"><html:checkbox
				property="terminationAction">Valid Termination Action</html:checkbox></td>
		</tr>

		<tr>
			<td class="NEXUSName">Backend Inbound Pipeline</td>
			<td class="NEXUSValue">
				<html:select property="backendInboundPipelineId">
					<logic:iterate name="choreographyActionForm" property="backendInboundPipelines" id="inbound">
						<html-el:option value="${inbound.nxPipelineId}">${inbound.name}</html-el:option>
					</logic:iterate>
				</html:select> 
			</td>
		</tr>
		<tr>
			<td class="NEXUSName">Backend Outbound Pipeline</td>
			<td class="NEXUSValue">
				<html:select property="backendOutboundPipelineId">
					<logic:iterate name="choreographyActionForm" property="backendOutboundPipelines" id="outbound">
						<html-el:option value="${outbound.nxPipelineId}">${outbound.name}</html-el:option>
					</logic:iterate>
				</html:select> 
			</td>
		</tr>
		<tr>
			<td class="NEXUSName">Status Update Pipeline</td>
			<td class="NEXUSValue">
				<html:select property="statusUpdatePipelineId">
					<html-el:option value="0">None</html-el:option>
					<logic:iterate name="choreographyActionForm" property="statusUpdatePipelines" id="inbound">
						<html-el:option value="${inbound.nxPipelineId}">${inbound.name}</html-el:option>
					</logic:iterate>
				</html:select> 
			</td>
		</tr>

	</table>
	<table class="NEXUS_TABLE" width="100%">
		<tr>
			<td class="NEXUSSection" colspan="2">Enabled Follow-Up Actions</td>
		</tr>
		<logic:iterate id="followup" property="followupActions"
			name="choreographyActionForm">
			<tr>
				<td class="NEXUSValue" colspan="2"><html-el:multibox
					name="choreographyActionForm" property="followups"
					value="${followup}" /><bean:write name="followup" /></td>
			</tr>
		</logic:iterate>

	</table>
</html:form>

	<table class="NEXUS_BUTTON_TABLE" width="100%">
		<tr>
			<td class="BUTTON_RIGHT"><nobr><nexus:submit form="document.forms[0]"><img src="images/submit.gif" class="button">Update</nexus:submit></nobr><nobr></td>

			<td class="BUTTON_RIGHT">
				<nobr>
				<nexus:submit onClick="document.forms[0].action='ActionDelete.do'; document.forms[0].nxChoreographyId=${choreographyActionForm.nxChoreographyId}; document.forms[0].nxActionId=${choreographyActionForm.nxActionId};" precondition="confirmDelete('Are you sure you want to delete this Action?')" form="document.forms[0]"><img src="images/submit.gif" class="button">Delete</nexus:submit>
				</nobr>
			</td>
		</tr>
	</table>
