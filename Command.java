package com.vox.meetup.gattic;

import android.content.Intent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

class Command
{
    static void execute(final ArrayList<String> data)
    {
        //execute a command
        String command=data.get(0);
        data.remove(0);

        /*
        * E: Error/logout
        * U: User info
        * P: Address info
        * A: Appointment info
        * B: Logout the user
        * D: Disconnect the user
        * G: Get confirmations/clientaccountids
        * C: Chat message
        * F: Friend info
        * R: Friend request response
        * N: Nvitations (lol)
        * Z: Tos prompt
        * K: Delete appointment
        * Q: New sid
        * L: Group Info
        * O: Group members
        * J: Delete group
        * V: Delete friend
        */

        //determine the commands
        switch(command)
        {
            case "E"://error/logout
            {
                if(Gattic.global.mainAct != null) Gattic.global.mainAct.logout(false);
                //also prompt some sort of errror message

                break;
            }
            case "U"://user info
            {
                //determine based on the id whether to update an existing
                //user or make a new one
                //U|accountid|field|value|...|...\|
                if(data.size() < 3) return;

                try
                {
                    //get the account id
                    long accountid=Integer.parseInt(data.get(0));

                    //get the other fields
                    Map<String, String> fieldData=new HashMap<String, String>();

                    for(int i=1;i<data.size();i+=2)
                        fieldData.put(data.get(i), data.get(i+1));

                    //set the fields
                    String newNumber="";
                    String newName="";

                    Iterator it=fieldData.entrySet().iterator();
                    while(it.hasNext())
                    {
                        Map.Entry pair=(Map.Entry)it.next();

                        if(pair.getKey().equals("number")) newNumber=pair.getValue().toString();
                        if(pair.getKey().equals("name")) newName=pair.getValue().toString();
                    }

                    boolean newUser=Gattic.global.accounts.get(accountid)==null;

                    if(newUser)
                    {
                        Account account=new Account(accountid, newNumber, newName);
                        Gattic.global.addAccount(account);
                    }
                    else//edit an existing user
                    {
                        if(!newNumber.isEmpty())
                        {
                            Gattic.global.accounts.get(accountid).setNumber(newNumber);

                            //update Data
                            if(Gattic.global.id == accountid)
                            {
                                Gattic.global.number=newNumber;
                                Gattic.global.saveData();
                            }
                        }
                        if(!newName.isEmpty()) Gattic.global.accounts.get(accountid).setName(newName);
                    }
                }
                catch(NumberFormatException ignored) { }

                break;
            }
            case "P"://address info
            {
                //determine based on the id whether to update an existing
                //address or make a new one
                //P|addressid|field|value|...|...\|
                if(data.size() < 3) return;

                try
                {
                    //get the address id
                    int addressid=Integer.parseInt(data.get(0));

                    //get the other fields
                    Map<String, String> fieldData=new HashMap<String, String>();

                    for(int i=1;i<data.size();i+=2)
                        fieldData.put(data.get(i), data.get(i+1));

                    //set the fields
                    long newAccountID=-1;
                    String newName="";
                    String newAptNo="";
                    String newStreet="";
                    String newCity="";
                    String newState="";
                    String newZip="";
                    String newCountry="";
                    float newLat=Address.LATLONG_DEFAULT;
                    float newLong=Address.LATLONG_DEFAULT;

                    Iterator it=fieldData.entrySet().iterator();
                    while(it.hasNext())
                    {
                        Map.Entry pair=(Map.Entry)it.next();

                        if(pair.getKey().equals("accountid")) newAccountID=Long.parseLong(pair.getValue().toString());
                        if(pair.getKey().equals("name")) newName=pair.getValue().toString();
                        if(pair.getKey().equals("aptno")) newAptNo=pair.getValue().toString();
                        if(pair.getKey().equals("street")) newStreet=pair.getValue().toString();
                        if(pair.getKey().equals("city")) newCity=pair.getValue().toString();
                        if(pair.getKey().equals("state")) newState=pair.getValue().toString();
                        if(pair.getKey().equals("zip")) newZip=pair.getValue().toString();
                        if(pair.getKey().equals("country")) newCountry=pair.getValue().toString();
                        if(pair.getKey().equals("latitude")) newLat=Float.parseFloat(pair.getValue().toString());
                        if(pair.getKey().equals("longitude")) newLong=Float.parseFloat(pair.getValue().toString());
                    }

                    boolean newAdd=Gattic.global.addresses.get(addressid)==null;

                    if(newAdd)
                    {
                        Address address=new Address(addressid, newAccountID,
                                newName, newAptNo, newStreet, newCity, newState, newZip, newCountry, newLat, newLong);
                        Gattic.global.addAddress(address);
                    }
                    else//edit an existing address
                    {
                        if(newAccountID != -1) Gattic.global.addresses.get(addressid).setAccountID(newAccountID);
                        if(!newName.isEmpty()) Gattic.global.addresses.get(addressid).setName(newName);
                        if(!newAptNo.isEmpty()) Gattic.global.addresses.get(addressid).setAptNo(newAptNo);
                        if(!newStreet.isEmpty()) Gattic.global.addresses.get(addressid).setStreet(newStreet);
                        if(!newCity.isEmpty()) Gattic.global.addresses.get(addressid).setCity(newCity);
                        if(!newState.isEmpty()) Gattic.global.addresses.get(addressid).setState(newState);
                        if(!newZip.isEmpty()) Gattic.global.addresses.get(addressid).setZip(newZip);
                        if(!newCountry.isEmpty()) Gattic.global.addresses.get(addressid).setCountry(newCountry);
                        if(newLat != Address.LATLONG_DEFAULT) Gattic.global.addresses.get(addressid).setLat(newLat);
                        if(newLong != Address.LATLONG_DEFAULT) Gattic.global.addresses.get(addressid).setLong(newLong);
                    }
                }
                catch(NumberFormatException ignored) { }

                break;
            }
            case "A"://appointment incoming
            {
                //determine based on the id whether to update an existing
                //appointment or make a new one
                //A|appointmentid|field|value|...|...\|
                if(data.size() < 3) return;

                try
                {
                    //get the appointment id
                    int appointmentid=Integer.parseInt(data.get(0));

                    //get the other fields
                    Map<String, String> fieldData=new HashMap<String, String>();

                    for(int i=1;i<data.size();i+=2)
                        fieldData.put(data.get(i), data.get(i+1));

                    //set the fields
                    long newAccountID=-1;
                    int newAddressID=-1;
                    int newStartTime=0;
                    int newEndTime=-1;
                    String newName="";
                    String newDesc="";

                    Iterator it=fieldData.entrySet().iterator();
                    while(it.hasNext())
                    {
                        Map.Entry pair=(Map.Entry)it.next();

                        if(pair.getKey().equals("masteraccountid")) newAccountID=Long.parseLong(pair.getValue().toString());
                        if(pair.getKey().equals("addressid")) newAddressID=Integer.parseInt(pair.getValue().toString());
                        if(pair.getKey().equals("starttime")) newStartTime=Integer.parseInt(pair.getValue().toString());
                        if(pair.getKey().equals("endtime")) newEndTime=Integer.parseInt(pair.getValue().toString());
                        if(pair.getKey().equals("name")) newName=pair.getValue().toString();
                        if(pair.getKey().equals("description")) newDesc=pair.getValue().toString();
                    }

                    boolean newApp=Gattic.global.appointments.get(appointmentid)==null;

                    if(newApp)
                    {
                        Appointment appointment=new Appointment(appointmentid, newAccountID, newAddressID, newStartTime, newEndTime, newName, newDesc);

                        //invite friends if we own it
                        if(Gattic.global.mainAct != null)
                        {
                            if((Gattic.global.id == newAccountID) && (Gattic.global.inviteAppName.contains(newName)))
                            {
                                Gattic.global.appointmentSelected=appointment;//need this
                                Gattic.global.classFlag=FLVItem.APPOINTMENT;
                                Gattic.global.mainAct.startActivity(new Intent(Gattic.global.mainAct, EventActivity.class));
                                Gattic.global.mainAct.startActivity(new Intent(Gattic.global.mainAct, InviteAppointmentActivity.class));
                                Gattic.global.inviteAppName.remove(newName);
                            }
                        }

                        //put it in the appointment list
                        Gattic.global.addAppointment(appointment);
                    }
                    else//edit an existing appointment
                    {
                        if(newAccountID != -1) Gattic.global.appointments.get(appointmentid).setAccountID(newAccountID);
                        if(newAddressID != -1) Gattic.global.appointments.get(appointmentid).setAddressID(newAddressID);
                        if(newStartTime != 0) Gattic.global.appointments.get(appointmentid).setStartTime(newStartTime);
                        if(newEndTime != -1)
                        {
                            Appointment appointment=Gattic.global.appointments.get(appointmentid);
                            appointment.setEndTime(newEndTime);
                            Gattic.global.updateAppLists();
                        }
                        if(!newName.isEmpty()) Gattic.global.appointments.get(appointmentid).setName(newName);
                        if(!newDesc.isEmpty()) Gattic.global.appointments.get(appointmentid).setDescription(newDesc);

                        Gattic.global.updateAppLists();
                    }

                }
                catch(NumberFormatException ignored) { }

                break;
            }
            case "B"://log the user out
            {
                if(Gattic.global.mainAct != null)
                    Gattic.global.mainAct.logout(false);

                break;
            }
            case "D"://disconnect
            {
                Gattic.global.gService.stopGService();

                break;
            }
            case "G"://Get confirmations/clientaccountids
            {
                if(data.size() < 2) return;

                //get the appointment id
                int appointmentid=Integer.parseInt(data.get(0));

                //client ids
                ArrayList<ClientAccount> newClientIDs=new ArrayList<ClientAccount>();

                for(int i=1;i<data.size();++i)
                {
                    String response=data.get(i);
                    int breakPoint=response.indexOf(',');
                    if(breakPoint == -1) continue;

                    //clientID
                    long newClientID=Long.parseLong(response.substring(0, breakPoint));
                    response=response.substring(breakPoint+1);
                    breakPoint=response.indexOf(',');
                    //confirmed
                    int confirmed=Integer.parseInt(response.substring(0, breakPoint));
                    response=response.substring(breakPoint+1);
                    breakPoint=response.indexOf(',');
                    //timeconfirmed
                    int timeconfirmed=Integer.parseInt(response.substring(breakPoint+1));

                    newClientIDs.add(new ClientAccount(newClientID, (confirmed==1), timeconfirmed));
                }

                //add the client ids to the appointment
                if(Gattic.global.appointments.get(appointmentid) != null)
                    Gattic.global.addClientAccount(Gattic.global.appointments.get(appointmentid), newClientIDs);

                break;
            }
            case "C"://Chat messages
            {
                if(data.size() < 4) return;

                //get the appointment id
                int appointmentid=Integer.parseInt(data.get(0));
                //get the sender account id
                long accountid=Long.parseLong(data.get(1));
                //get the timestamp
                int timestamp=Integer.parseInt(data.get(2));
                //get the message
                String message=data.get(3);

                //add the message
                if(Gattic.global.appointments.get(appointmentid) != null)
                {
                    Gattic.global.addMessage(Gattic.global.appointments.get(appointmentid), new ChatMessage(accountid, message, timestamp));

                    //notificaiton
                    boolean notification=false;
                    if(Gattic.global.isService)
                    {
                        if(!Gattic.global.isIPCBound())
                            notification=true;
                    }
                    else//not a service
                    {
                        //short circuiting at its finest
                        if((Gattic.global.eventAct == null) || (Gattic.global.appointments.get(appointmentid).getID() != appointmentid))
                            notification=true;
                    }

                    if(notification)
                        Gattic.global.notification(Gattic.global.appointments.get(appointmentid).getName(), message, 1, appointmentid);
                }

                break;
            }
            case "F"://Friend info
            {
                if(data.size() < 3) return;

                //get the friend id
                int friendid=Integer.parseInt(data.get(0));
                //get the confirmed status
                int confirmed=Integer.parseInt(data.get(1));
                //get the sender status
                int sender=Integer.parseInt(data.get(2));

                Friend newFriend=new Friend(friendid, (confirmed==1), (sender==1));
                Gattic.global.addFriend(newFriend);

                //notificaiton
                //replace the last arg for the specific friend, different nId/notification though
                if((Gattic.global.isService) && (newFriend.status() == Friend.SENDER))
                    Gattic.global.notification("Gattic Friend Request", "New Incoming Friend Requests!", 2, 0);

                break;
            }
            case "R"://Friend request response
            {
                if(data.size() < 2) return;

                //get the friend number
                String friendNumber=data.get(0);
                //get the found status
                int success=Integer.parseInt(data.get(1));

                if(Gattic.global.mainAct != null)
                    Gattic.global.mainAct.popup(success==1? "Friend request sent!":friendNumber+" not found!");

                break;
            }
            case "N":
            {
                if(data.size() < 1) return;

                //invitations
                for(int i=0;i<data.size();++i)
                {
                    String response=data.get(i);
                    int breakPoint=response.indexOf(',');
                    if(breakPoint == -1) continue;

                    //appointmentid
                    int newAppointmentID=Integer.parseInt(response.substring(0, breakPoint));
                    response=response.substring(breakPoint+1);
                    breakPoint=response.indexOf(',');
                    //confirmed
                    int confirmed=Integer.parseInt(response.substring(0, breakPoint));
                    response=response.substring(breakPoint+1);
                    breakPoint=response.indexOf(',');
                    //timeconfirmed
                    int timeconfirmed=Integer.parseInt(response.substring(breakPoint+1));

                    //add it
                    Confirmation newConfirmation=new Confirmation(newAppointmentID, (confirmed==1), timeconfirmed);
                    if(Gattic.global.invites.get(newConfirmation.getID()) == null)
                        Gattic.global.addInvitation(newConfirmation);

                    //notificaiton
                    Appointment newApp=Gattic.global.appointments.get(newConfirmation.getID());
                    if(newApp != null)
                    {
                        String appName=newApp.getName();
                        if((Gattic.global.isService) && (newConfirmation.status() == Confirmation.STATUS_UNCONFIRMED))
                            Gattic.global.notification("Gattic Event Invitation", "Invitation to join "+appName, 1, newConfirmation.getID());
                    }
                }

                break;
            }
            case "Z"://tos prompt
            {
                if(Gattic.global.mainAct != null)
                {
                    Gattic.global.TOS=false;
                    Intent intent=new Intent(Gattic.global.mainAct, ToSActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NO_HISTORY);
                    Gattic.global.mainAct.startActivity(intent);
                }

                break;
            }
            case "K"://delete appointment
            {
                if(data.size() < 1) return;

                //get appointment id
                int appointmentid=Integer.parseInt(data.get(0));

                Gattic.global.removeAppointment(appointmentid);

                break;
            }
            case "Q"://new sid
            {
                if(data.size() < 1) return;

                //get the new sid
                Gattic.global.sid=data.get(0);

                break;
            }
            case "L"://Group
            {
                //improve this when there is more than just a name
                if(data.size() < 3) return;

                //get the group id
                int groupid=Integer.parseInt(data.get(0));

                //get the master account id
                long masteraccountid=Long.parseLong(data.get(1));

                //get the group name
                String groupName=data.get(2);

                //get the group description
                String groupDesc="";
                if(data.size() > 3)
                {
                    //get the group desc
                    groupDesc=data.get(3);
                }

                Gattic.global.addGroup(new Group(groupid, groupName, groupDesc, masteraccountid));

                break;
            }
            case "O"://Group Members
            {
                if(data.size() < 2) return;

                //get the group id
                int groupid=Integer.parseInt(data.get(0));

                //client ids
                ArrayList<ClientAccount> newClientIDs=new ArrayList<ClientAccount>();

                for(int i=1;i<data.size();++i)
                {
                    String response=data.get(i);
                    int breakPoint=response.indexOf(',');
                    if(breakPoint == -1) continue;

                    //clientID
                    long newClientID=Long.parseLong(response.substring(0, breakPoint));
                    response=response.substring(breakPoint+1);
                    breakPoint=response.indexOf(',');
                    //confirmed
                    int confirmed=Integer.parseInt(response.substring(0, breakPoint));
                    response=response.substring(breakPoint+1);
                    breakPoint=response.indexOf(',');
                    //timeconfirmed
                    int timeconfirmed=Integer.parseInt(response.substring(breakPoint+1));

                    newClientIDs.add(new ClientAccount(newClientID, (confirmed==1), timeconfirmed));
                }

                //add the client ids to the group
                if(Gattic.global.groups.get(groupid) != null)
                    Gattic.global.addGroupClientAccount(Gattic.global.groups.get(groupid), newClientIDs);

                break;
            }
            case "J"://Delete group
            {
                if(data.size() < 1) return;

                //get the group id
                int groupid=Integer.parseInt(data.get(0));
                Gattic.global.removeGroup(groupid);

                break;
            }
            case "V"://Delete friend
            {
                if(data.size() < 1) return;

                //get the friend id
                long friendid=Long.parseLong(data.get(0));
                Gattic.global.removeFriend(friendid);

                break;
            }
        }
    }
}
