/*
 * Copyright (c) 2015 Christian Chevalley
 * This file is part of Project Ethercis
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ethercis.persistence;

import com.ethercis.dao.access.interfaces.I_DomainAccess;
import com.ethercis.dao.access.interfaces.I_PartyIdentifiedAccess;
import com.ethercis.dao.access.interfaces.I_SystemAccess;
import com.ethercis.logonservice.session.I_SessionManager;
import com.ethercis.servicemanager.cluster.ClusterInfo;
import com.ethercis.servicemanager.cluster.RunTimeSingleton;
import com.ethercis.servicemanager.common.I_SessionClientProperties;
import com.ethercis.servicemanager.common.identification.IdentificationDef;
import com.ethercis.servicemanager.exceptions.ServiceManagerException;

import java.util.UUID;

/**
 * Created by christian on 12/18/2015.
 */
public class AuditSetter {
    String COMMITTER_ID = "committerId" ;
    String COMMITTER_NAME = "committerName";
    String DESCRIPTION = "description";
    private IdentificationDef identificationDef;
    private RunTimeSingleton global;

    String committerId;
    String committerName;
    String description;

    String sessionId;
    UUID committerUuid;
    UUID systemUuid;

    public AuditSetter(RunTimeSingleton global) {
        this.global = global;
        this.identificationDef = new IdentificationDef(global);
    }

    public void handleProperties(I_DomainAccess domainAccess, I_SessionClientProperties props) throws ServiceManagerException {
        sessionId = props.getClientProperty(I_SessionManager.SECRET_SESSION_ID_INTERNAL, (String)null);
        committerId = props.getClientProperty(COMMITTER_ID, getSessionSubjectId(sessionId));
        committerName = props.getClientProperty(COMMITTER_NAME, getSessionSubjectName(sessionId));
        description = props.getClientProperty(DESCRIPTION, (String)null);
        committerUuid = getCommitter(domainAccess, committerName, committerId);
        systemUuid = getOrCreateSystemId(domainAccess, committerName+"-session", committerName+"@"+props.getClientProperty(I_SessionManager.CLIENT_IP, "LOCAL"));
    }

    //TODO: refactor into IdentificationService
    public UUID getCommitter(I_DomainAccess domainAccess, String name, String id){
        UUID committerId = I_PartyIdentifiedAccess.getOrCreatePartyByExternalRef(domainAccess, name, id,
                identificationDef.getIdenficationScheme(),
                identificationDef.getIdendificationNamespace(),
                identificationDef.getIdenficationType());
        return committerId;
    }

    public String getSessionSubjectName(String sessionId) throws ServiceManagerException {
        I_SessionManager sessionManager = ClusterInfo.getRegisteredService(global, "LogonService", "1.0");
        //retrieve the session manager
        return sessionManager.getSubjectName(sessionId);
    }

    public String getSessionSubjectId(String sessionId) throws ServiceManagerException {
        I_SessionManager sessionManager = ClusterInfo.getRegisteredService(global, "LogonService", "1.0");
        //retrieve the session manager
        return sessionManager.getSubjectId(sessionId);
    }

    //this is to avoid circular references
    public UUID getOrCreateSystemId(I_DomainAccess domainAccess, String description, String systemAddress){
        UUID systemId = null;
        try {
            systemId = I_SystemAccess.retrieveInstanceId(domainAccess, systemAddress);
        } catch (Exception e){
            ;
        }

        if (systemId == null){
            try {
                I_SystemAccess systemAccess = I_SystemAccess.getInstance(domainAccess, description, systemAddress);
                systemId = systemAccess.commit();
            }
            catch (Exception e){
                throw new IllegalArgumentException("Could not create client with settings:"+systemAddress);
            }
        }

        return systemId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public UUID getCommitterUuid() {
        return committerUuid;
    }

    public UUID getSystemUuid() {
        return systemUuid;
    }

    public String getDescription() {
        return description;
    }
}
