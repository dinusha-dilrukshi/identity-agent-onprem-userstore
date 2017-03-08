/**
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 * <p>
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.carbon.identity.agent.onprem.userstore.manager.claim;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.wso2.carbon.identity.agent.onprem.userstore.config.ClaimConfiguration;
import org.wso2.carbon.identity.agent.onprem.userstore.manager.common.UserStoreManager;
import org.wso2.carbon.identity.agent.onprem.userstore.manager.common.UserStoreManagerBuilder;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;


/**
 * Do custom modifications to the user claims.
 */
public class CustomClaimModifier {

    private static Log log = LogFactory.getLog(CustomClaimModifier.class);

    private static Map<String, String> sharedGroupsMap = new LinkedHashMap<String, String>();

    private static final String SHARED_ACCOUNT_FILE = "shared-accounts.xml";

    private static final String sharedAccountXMLFilePath = "conf/";

    private static  final String SALESFORCE_SHARED_ACCOUNT_CLAIM = "http://wso2.org/claims/salesforcesharedaccount";

    /*
     * Reading the shared groups and shared usernames from the shared-accounts.xml in <AGENT_HOME>/conf
     */
    static {

        try {
            File sharedAccountFile = new File(sharedAccountXMLFilePath, SHARED_ACCOUNT_FILE);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(sharedAccountFile);
            doc.getDocumentElement().normalize();

            NodeList nList = doc.getElementsByTagName("SharedGroup");

            for (int i = 0; i < nList.getLength(); i++) {
                Node nNode = nList.item(i);

                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) nNode;
                    sharedGroupsMap.put(eElement.getElementsByTagName("groupName").item(0).getTextContent(), eElement
                            .getElementsByTagName("sharedEmail").item(0).getTextContent());
                }
            }
        } catch (Exception e) {
            log.error("Erro in reading contents from shared-accounts.xml" + e.getMessage());
        }
    }


    /**
     * Add a new claim to represent salesforce shared account of user to user's claim map.
     * @param userClaims Map containing the name value pairs of user's claims.
     * @param username Username of the user.
     * @return Modified claims map for given user, with custom claims.
     */
    public Map<String, String> setSharedAccountForSalesforce(Map<String, String> userClaims, String
            username) {

        //Find mapped attribute for "http://wso2.org/claims/salesforcesharedaccount" claim
        ClaimManager claimManager = new ClaimManager(ClaimConfiguration.getConfiguration().getClaimMap());
        Map<String, String> claimAttributeMap = claimManager.getClaimAttributes();

        if (claimAttributeMap != null) {
            String mappedAttributeOfSalesforce = claimAttributeMap.get(SALESFORCE_SHARED_ACCOUNT_CLAIM);
            if (mappedAttributeOfSalesforce != null) {
                userClaims.put(mappedAttributeOfSalesforce, changeEmailforSalesforce(username));
            }
        }

        return userClaims;
    }


    /**
     * Change the Username to the shared username for users in the shared groups.
     * @param username Username of the user.
     * @return Shared email address for given user.
     */
    private String changeEmailforSalesforce(String username) {

            try {
                UserStoreManager userStoreManager = UserStoreManagerBuilder.getUserStoreManager();
                String[]  rolesofUser = userStoreManager.doGetExternalRoleListOfUser(username);
                Arrays.sort(rolesofUser);
                for (String role : sharedGroupsMap.keySet()) {
                    if (Arrays.binarySearch(rolesofUser, role) > -1) {
                        username = sharedGroupsMap.get(role);
                        break;
                    }
                }
            } catch (Exception e) {
                log.error("Error when changing the username for Salesforce Service Provider" + e.getMessage());
            }

        return username;
    }
}
