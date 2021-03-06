/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.globaldisplaydetails;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.globalstatus.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.account.*;

import org.jitsi.service.configuration.*;

import java.util.*;

/**
 * Global statuses service impl. Gives access to the outside for some
 * methods to change the global status and individual protocol provider
 * status. Use to implement global status menu with list of all account
 * statuses.
 * @author Damian Minkov
 */
public class GlobalStatusServiceImpl
    implements GlobalStatusService
{
    /**
     * The object used for logging.
     */
    private final Logger logger
        = Logger.getLogger(GlobalStatusServiceImpl.class);

    /**
     * Returns the last status that was stored in the configuration for the
     * given protocol provider.
     *
     * @param protocolProvider the protocol provider
     * @return the last status that was stored in the configuration for the
     *         given protocol provider
     */
    public PresenceStatus getLastPresenceStatus(
        ProtocolProviderService protocolProvider)
    {
        String lastStatus = getLastStatusString(protocolProvider);

        PresenceStatus status = null;

        if (lastStatus != null)
        {
            OperationSetPresence presence
                = protocolProvider.getOperationSet(OperationSetPresence.class);

            if (presence == null)
                return null;

            Iterator<PresenceStatus> i = presence.getSupportedStatusSet();

            // Check if there's such status in the supported presence status
            // set.
            while (i.hasNext())
            {
                PresenceStatus nextStatus = i.next();

                if (nextStatus.getStatusName().equals(lastStatus))
                    status = nextStatus;
            }

            // If we haven't found the last status in the protocol provider
            // supported status set, we'll have a look for a corresponding
            // global status and its protocol representation.
            if (status == null)
            {
                if (lastStatus.equals(GlobalStatusEnum.ONLINE_STATUS))
                {
                    status = getPresenceStatus(
                        protocolProvider,
                        PresenceStatus.AVAILABLE_THRESHOLD,
                        PresenceStatus.EAGER_TO_COMMUNICATE_THRESHOLD);
                }
                else if (lastStatus.equals(GlobalStatusEnum.AWAY_STATUS))
                {
                    status = getPresenceStatus(
                        protocolProvider,
                        PresenceStatus.AWAY_THRESHOLD,
                        PresenceStatus.AVAILABLE_THRESHOLD);
                }
                else if (lastStatus
                            .equals(GlobalStatusEnum.DO_NOT_DISTURB_STATUS))
                {
                    status = getPresenceStatus(
                            protocolProvider,
                            PresenceStatus.ONLINE_THRESHOLD,
                            PresenceStatus.AWAY_THRESHOLD);
                }
                else if (lastStatus
                            .equals(GlobalStatusEnum.FREE_FOR_CHAT_STATUS))
                {
                    status = getPresenceStatus(
                            protocolProvider,
                            PresenceStatus.AVAILABLE_THRESHOLD,
                            PresenceStatus.MAX_STATUS_VALUE);
                }
                else if (lastStatus.equals(GlobalStatusEnum.OFFLINE_STATUS))
                {
                    status = getPresenceStatus(
                            protocolProvider,
                            0,
                            GlobalStatusEnum.ONLINE_THRESHOLD);
                }
            }
        }
        return status;
    }

    /**
     * Returns the last contact status saved in the configuration.
     *
     * @param protocolProvider the protocol provider to which the status
     *            corresponds
     * @return the last contact status saved in the configuration.
     */
    public String getLastStatusString(ProtocolProviderService protocolProvider)
    {
        // find the last contact status saved in the configuration.
        String lastStatus = null;

        ConfigurationService configService
            = GlobalDisplayDetailsActivator.getConfigurationService();
        String prefix = "net.java.sip.communicator.impl.gui.accounts";
        List<String> accounts
            = configService.getPropertyNamesByPrefix(prefix, true);
        String protocolProviderAccountUID
            = protocolProvider.getAccountID().getAccountUniqueID();

        for (String accountRootPropName : accounts)
        {
            String accountUID = configService.getString(accountRootPropName);

            if (accountUID.equals(protocolProviderAccountUID))
            {
                lastStatus =
                    configService.getString(accountRootPropName
                        + ".lastAccountStatus");

                if (lastStatus != null)
                    break;
            }
        }

        return lastStatus;
    }

    /**
     * Publish present status. We search for the highest value in the
     * given interval.
     *
     * @param protocolProvider the protocol provider to which we
     * change the status.
     * @param status the status tu publish.
     */
    public void publishStatus(
            ProtocolProviderService protocolProvider,
            PresenceStatus status,
            boolean rememberStatus)
    {
        OperationSetPresence presence
                = protocolProvider.getOperationSet(OperationSetPresence.class);

        LoginManager loginManager
            = GlobalDisplayDetailsActivator.getUIService().getLoginManager();
        RegistrationState registrationState
            = protocolProvider.getRegistrationState();

        if (registrationState == RegistrationState.REGISTERED
            && presence != null
            && !presence.getPresenceStatus().equals(status))
        {
            if (status.isOnline())
            {
                new PublishPresenceStatusThread(
                        protocolProvider,
                        presence,
                        status).start();
            }
            else
            {
                loginManager.setManuallyDisconnected(true);
                LoginManager.logoff(protocolProvider);
            }
        }
        else if (registrationState != RegistrationState.REGISTERED
            && registrationState != RegistrationState.REGISTERING
            && registrationState != RegistrationState.AUTHENTICATING
            && status.isOnline())
        {
            GlobalDisplayDetailsActivator.getUIService().getLoginManager()
                .login(protocolProvider);
        }
        else if (!status.isOnline()
                && !(registrationState
                        == RegistrationState.UNREGISTERING))
        {
            loginManager.setManuallyDisconnected(true);
            LoginManager.logoff(protocolProvider);
        }

        if(rememberStatus)
            saveStatusInformation(
                protocolProvider,
                status.getStatusName());
    }

    /**
     * Publish present status. We search for the highest value in the
     * given interval.
     *
     * change the status.
     * @param globalStatus
     */
    public void publishStatus(GlobalStatusEnum globalStatus)
    {
        String itemName = globalStatus.getStatusName();

        Iterator<ProtocolProviderService> pProviders
            = AccountUtils.getRegisteredProviders().iterator();

        while (pProviders.hasNext())
        {
            ProtocolProviderService protocolProvider = pProviders.next();

            if(itemName.equals(GlobalStatusEnum.ONLINE_STATUS))
            {
                if(!protocolProvider.isRegistered())
                {
                    saveStatusInformation(protocolProvider, itemName);

                    GlobalDisplayDetailsActivator.getUIService()
                        .getLoginManager().login(protocolProvider);
                }
                else
                {
                    OperationSetPresence presence
                        = protocolProvider
                            .getOperationSet(OperationSetPresence.class);

                    if (presence == null)
                    {
                        saveStatusInformation(protocolProvider, itemName);

                        continue;
                    }

                    Iterator<PresenceStatus> statusSet
                        = presence.getSupportedStatusSet();

                    while (statusSet.hasNext())
                    {
                        PresenceStatus status = statusSet.next();

                        if( status.getStatus()
                                < PresenceStatus.EAGER_TO_COMMUNICATE_THRESHOLD
                            && status.getStatus()
                                >= PresenceStatus.AVAILABLE_THRESHOLD)
                        {
                            new PublishPresenceStatusThread(protocolProvider,
                                                            presence, status)
                                .start();

                            this.saveStatusInformation( protocolProvider,
                                                        status.getStatusName());

                            break;
                        }
                    }
                }
            }
            else if (itemName.equals(GlobalStatusEnum.OFFLINE_STATUS))
            {
                if(    !protocolProvider.getRegistrationState()
                                .equals(RegistrationState.UNREGISTERED)
                    && !protocolProvider.getRegistrationState()
                                .equals(RegistrationState.UNREGISTERING))
                {
                    OperationSetPresence presence
                        = protocolProvider
                            .getOperationSet(OperationSetPresence.class);

                    if (presence == null)
                    {
                        saveStatusInformation(  protocolProvider,
                                                itemName);

                        LoginManager.logoff(protocolProvider);

                        continue;
                    }

                    Iterator<PresenceStatus> statusSet
                        = presence.getSupportedStatusSet();

                    while (statusSet.hasNext())
                    {
                        PresenceStatus status = statusSet.next();

                        if(status.getStatus()
                            < PresenceStatus.ONLINE_THRESHOLD)
                        {
                            this.saveStatusInformation( protocolProvider,
                                status.getStatusName());

                            break;
                        }
                    }

                    try
                    {
                        protocolProvider.unregister();
                    }
                    catch (OperationFailedException e1)
                    {
                        logger.error(
                            "Unable to unregister the protocol provider: "
                            + protocolProvider
                            + " due to the following exception: " + e1);
                    }
                }
            }
            else if (itemName.equals(GlobalStatusEnum.FREE_FOR_CHAT_STATUS))
            {
                if(!protocolProvider.isRegistered())
                {
                    saveStatusInformation(protocolProvider, itemName);

                    GlobalDisplayDetailsActivator.getUIService()
                        .getLoginManager().login(protocolProvider);
                }
                else
                    // we search for highest available status here
                    publishStatus(
                            protocolProvider,
                            PresenceStatus.AVAILABLE_THRESHOLD,
                            PresenceStatus.MAX_STATUS_VALUE);
            }
            else if (itemName.equals(GlobalStatusEnum.DO_NOT_DISTURB_STATUS))
            {
                if(!protocolProvider.isRegistered())
                {
                    saveStatusInformation(protocolProvider, itemName);

                    GlobalDisplayDetailsActivator.getUIService()
                        .getLoginManager().login(protocolProvider);
                }
                else
                    // status between online and away is DND
                    publishStatus(
                            protocolProvider,
                            PresenceStatus.ONLINE_THRESHOLD,
                            PresenceStatus.AWAY_THRESHOLD);
            }
            else if (itemName.equals(GlobalStatusEnum.AWAY_STATUS))
            {
                if(!protocolProvider.isRegistered())
                {
                    saveStatusInformation(protocolProvider, itemName);

                    GlobalDisplayDetailsActivator.getUIService()
                        .getLoginManager().login(protocolProvider);
                }
                else
                    // a status in the away interval
                    publishStatus(
                            protocolProvider,
                            PresenceStatus.AWAY_THRESHOLD,
                            PresenceStatus.AVAILABLE_THRESHOLD);
            }
        }
    }

    /**
     * Publish present status. We search for the highest value in the
     * given interval.
     *
     * @param protocolProvider the protocol provider to which we
     * change the status.
     * @param floorStatusValue the min status value.
     * @param ceilStatusValue the max status value.
     */
    private void publishStatus(
            ProtocolProviderService protocolProvider,
            int floorStatusValue, int ceilStatusValue)
    {
        if (!protocolProvider.isRegistered())
            return;

        PresenceStatus status = getPresenceStatus(  protocolProvider,
                                                    floorStatusValue,
                                                    ceilStatusValue);

        if (status != null)
        {
            OperationSetPresence presence
                = protocolProvider
                    .getOperationSet(OperationSetPresence.class);

            new PublishPresenceStatusThread(protocolProvider, presence, status)
                .start();

            this.saveStatusInformation( protocolProvider,
                                        status.getStatusName());
        }
    }

    private PresenceStatus getPresenceStatus(
        ProtocolProviderService protocolProvider,
        int floorStatusValue,
        int ceilStatusValue)
    {
        OperationSetPresence presence
            = protocolProvider
                .getOperationSet(OperationSetPresence.class);

        if (presence == null)
            return null;

        Iterator<PresenceStatus> statusSet
            = presence.getSupportedStatusSet();

        PresenceStatus status = null;

        while (statusSet.hasNext())
        {
            PresenceStatus currentStatus = statusSet.next();

            if (status == null
                && currentStatus.getStatus() < ceilStatusValue
                && currentStatus.getStatus() >= floorStatusValue)
            {
                status = currentStatus;
            }

            if (status != null)
            {
                if (currentStatus.getStatus() < ceilStatusValue
                    && currentStatus.getStatus() >= floorStatusValue
                    && currentStatus.getStatus() > status.getStatus())
                {
                    status = currentStatus;
                }
            }
        }

        return status;
    }

    /**
     * Saves the last status for all accounts. This information is used
     * on loging. Each time user logs in he's logged with the same status
     * as he was the last time before closing the application.
     * @param protocolProvider the protocol provider to save status information
     * for
     * @param statusName the name of the status to save
     */
    private void saveStatusInformation(
            ProtocolProviderService protocolProvider,
            String statusName)
    {
        ConfigurationService configService
            = GlobalDisplayDetailsActivator.getConfigurationService();

        String prefix = "net.java.sip.communicator.impl.gui.accounts";

        List<String> accounts = configService
                .getPropertyNamesByPrefix(prefix, true);

        boolean savedAccount = false;

        for (String accountRootPropName : accounts) {
            String accountUID
                = configService.getString(accountRootPropName);

            if(accountUID.equals(protocolProvider
                    .getAccountID().getAccountUniqueID())) {

                configService.setProperty(
                        accountRootPropName + ".lastAccountStatus",
                        statusName);

                savedAccount = true;
            }
        }

        if(!savedAccount)
        {
            String accNodeName
                = "acc" + Long.toString(System.currentTimeMillis());

            String accountPackage
                = "net.java.sip.communicator.impl.gui.accounts."
                        + accNodeName;

            configService.setProperty(accountPackage,
                protocolProvider.getAccountID().getAccountUniqueID());

            configService.setProperty(
                accountPackage + ".lastAccountStatus",
                statusName);
        }
    }

    /**
     * Publishes the given status to the given presence operation set.
     */
    private class PublishPresenceStatusThread
        extends Thread
    {
        private ProtocolProviderService protocolProvider;

        private PresenceStatus status;

        private OperationSetPresence presence;

        /**
         * Publishes the given <tt>status</tt> through the given
         * <tt>presence</tt> operation set.
         * @param presence the operation set through which we publish the status
         * @param status the status to publish
         */
        public PublishPresenceStatusThread(
                                    ProtocolProviderService protocolProvider,
                                    OperationSetPresence presence,
                                    PresenceStatus status)
        {
            this.protocolProvider = protocolProvider;
            this.presence = presence;
            this.status = status;
        }

        @Override
        public void run()
        {
            try
            {
                presence.publishPresenceStatus(status, "");
            }
            catch (IllegalArgumentException e1)
            {

                logger.error("Error - changing status", e1);
            }
            catch (IllegalStateException e1)
            {

                logger.error("Error - changing status", e1);
            }
            catch (OperationFailedException e1)
            {
                if (e1.getErrorCode()
                    == OperationFailedException.GENERAL_ERROR)
                {
                    String msgText =
                        GlobalDisplayDetailsActivator.getResources()
                            .getI18NString(
                            "service.gui.STATUS_CHANGE_GENERAL_ERROR",
                            new String[]{
                                protocolProvider.getAccountID().getUserID(),
                                protocolProvider.getAccountID().getService()});

                    GlobalDisplayDetailsActivator.getAlertUIService()
                        .showAlertDialog(
                            GlobalDisplayDetailsActivator.getResources()
                                .getI18NString("service.gui.GENERAL_ERROR"),
                            msgText,
                            e1);
                }
                else if (e1.getErrorCode()
                    == OperationFailedException.NETWORK_FAILURE)
                {
                    String msgText =
                        GlobalDisplayDetailsActivator.getResources()
                            .getI18NString(
                            "service.gui.STATUS_CHANGE_NETWORK_FAILURE",
                            new String[]{
                                protocolProvider.getAccountID().getUserID(),
                                protocolProvider.getAccountID().getService()});

                    GlobalDisplayDetailsActivator.getAlertUIService()
                        .showAlertDialog(
                            msgText,
                            GlobalDisplayDetailsActivator.getResources()
                                .getI18NString("service.gui.NETWORK_FAILURE"),
                            e1);
                }
                else if (e1.getErrorCode()
                        == OperationFailedException.PROVIDER_NOT_REGISTERED)
                {
                    String msgText =
                        GlobalDisplayDetailsActivator.getResources()
                            .getI18NString(
                            "service.gui.STATUS_CHANGE_NETWORK_FAILURE",
                            new String[]{
                                protocolProvider.getAccountID().getUserID(),
                                protocolProvider.getAccountID().getService()});

                    GlobalDisplayDetailsActivator.getAlertUIService()
                    .showAlertDialog(
                        GlobalDisplayDetailsActivator.getResources()
                            .getI18NString("service.gui.NETWORK_FAILURE"),
                        msgText,
                        e1);
                }
                logger.error("Error - changing status", e1);
            }
        }
    }
}
