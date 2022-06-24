package com.vox.meetup.gattic;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;

import java.util.ArrayList;

//returns the instance of the service
//InterProcess Communication
class IPC extends Handler
{
    //process detection
    static final int SOCKETSHMOCKETS=0;
    static final int UI=1;

    //for the IPC
    static final int INITIAL_SERVICE_REPLY=0;
    static final int UI2SERVICE=1;
    static final int SERVICE2UI=2;
    static final int LOGINACT_FAIL=3;
    static final int LOGINACT_SUCCESS=4;
    static final int REGACT_FAIL=5;
    static final int REGACT_SUCCESS=6;
    static final int QUICKLOGIN_FAIL=7;
    static final int QUICKLOGIN_SUCCESS=8;
    static final int MAINACT_FAIL=9;
    static final int MAINACT_SUCCESS=10;
    static final int LOGOUT=11;
    static final int RECONNECT=12;
    static final int RECONNECT_LAYOUT=13;
    static final int CLIENT_SOCK_UPDATE=14;

    @Override
    public void handleMessage(Message msg)
    {
        Bundle bundle=msg.getData();
        Object obj=bundle.get("wData");
        ArrayList<String> data=null;
        if(!(obj instanceof ArrayList)) return;
        data=(ArrayList<String>)obj;

        Gattic.global.mMessengerIPC=msg.replyTo;

        switch(msg.what)
        {
            case INITIAL_SERVICE_REPLY:

                //now the service has a messenger!
                //populate the ui
                if(Gattic.global.isService)
                {
                    //if this isnt our first UI
                    if(Gattic.global.gService.recvLog.size() > 0)
                    {
                        //the login
                        writeIPC(Gattic.global.gService.recvLog.get(0), QUICKLOGIN_SUCCESS, UI);

                        //the rest
                        for(int i=1;i<Gattic.global.gService.recvLog.size();++i)
                            writeIPC(Gattic.global.gService.recvLog.get(i), SERVICE2UI, UI);
                    }
                }

                break;

            case UI2SERVICE:

                if(Gattic.global.isService)
                {
                    //write to the service
                    Sockets.writeEngine(data);
                }

                break;

            case SERVICE2UI:

                if(!Gattic.global.isService)
                {
                    //do our thing
                    Command.execute(data);
                }

                break;

            //The ui connection updates
            case LOGINACT_FAIL:

                if((!Gattic.global.isService) && (Gattic.global.loginAct != null))
                    Gattic.global.loginAct.loginFailed();

                break;

            case LOGINACT_SUCCESS:

                if((!Gattic.global.isService) && (Gattic.global.loginAct != null))
                    Gattic.global.loginAct.loginSuccessful(data);

                break;

            case REGACT_FAIL:

                if((!Gattic.global.isService) && (Gattic.global.regAct != null))
                    Gattic.global.regAct.registerFailed();

                break;

            case REGACT_SUCCESS:

                if((!Gattic.global.isService) && (Gattic.global.regAct != null))
                    Gattic.global.regAct.registerSuccessful(data);

                break;

            case QUICKLOGIN_FAIL:

                if((!Gattic.global.isService) && (Gattic.global.splashAct != null))
                    Gattic.global.splashAct.splashFailed();

                break;

            case QUICKLOGIN_SUCCESS:

                if((!Gattic.global.isService) && (Gattic.global.splashAct != null))
                    Gattic.global.splashAct.splashSuccessful(data);

                break;

            case MAINACT_FAIL:

                if((!Gattic.global.isService) && (Gattic.global.mainAct != null))
                    Gattic.global.mainAct.loginFailed();

                break;

            case MAINACT_SUCCESS:

                if((!Gattic.global.isService) && (Gattic.global.mainAct != null))
                    Gattic.global.mainAct.loginSuccessful(data);

                break;

            //stop the service
            case LOGOUT:

                //stop the service
                if(Gattic.global.isService)
                {
                    //"logout"
                    //Gattic.global.gService.recvLog.clear();
                    //Gattic.global.gService.writeLog.clear();
                    Gattic.global.gService.stopGService();
                    Gattic.global.logout();
                }

                break;

            case RECONNECT:

                if(!Gattic.global.isService)
                    GatticService.connect(data, Sockets.RECONNECT);

                break;

            case RECONNECT_LAYOUT:

                if(data.size() < 2) break;

                if(!Gattic.global.isService)
                {
                    //get the reconnection text
                    final String recText=data.get(0);
                    //get the reconnection color
                    final int recColor=Integer.parseInt(data.get(1));
                    //get the visibility
                    final boolean visibility=data.get(2).equals("1");

                    //set the connection text and visibility
                    if(Gattic.global.mainAct != null)
                    {
                        Gattic.global.mainAct.runOnUiThread(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                Gattic.global.mainAct.setConnectionText(recText, recColor, visibility);
                            }
                        });
                    }
                }
                else
                    writeIPC(data, RECONNECT_LAYOUT, UI);

                break;

            case CLIENT_SOCK_UPDATE:

                if(data.size() < 1) break;

                if(!Gattic.global.isService)
                    Gattic.global.socks.status=Integer.parseInt(data.get(0));
                else
                    writeIPC(data, CLIENT_SOCK_UPDATE, UI);

                break;

            default:
                super.handleMessage(msg);
        }
    }

    static boolean writeIPC(final ArrayList<String> wData, final int ACTION, final int processFlag)//both processes will run this
    {
        //We can message locally within out own process (manually call handleMessage)
        boolean localMessage=false;
        if(((processFlag == SOCKETSHMOCKETS) && (Gattic.global.isService))
                || ((processFlag == UI) && (!Gattic.global.isService)))
            localMessage=true;
        else//InterProcess Communication
        {
            //there might not be a client
            if(Gattic.global.mMessengerIPC == null) return false;
        }

        //write to the other process
        Message msg=Message.obtain(null, ACTION);
        msg.replyTo=Gattic.global.mMessengerMain;

        //set the bundle and send
        Bundle bundle=new Bundle();
        bundle.putStringArrayList("wData", wData);
        msg.setData(bundle);

        try
        {
            if(localMessage)
                Gattic.global.mMessengerMain.send(msg);
            else
            {
                if(Gattic.global.mMessengerIPC != null)
                    Gattic.global.mMessengerIPC.send(msg);
            }
        }
        catch(RemoteException ex)
        {
            Gattic.global.mMessengerIPC=null;
            return false;
        }

        //success!
        return true;
    }
}
