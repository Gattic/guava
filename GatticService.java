package com.vox.meetup.gattic;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.v4.util.Pair;

import java.util.ArrayList;

public class GatticService extends Service
{
    //for running
    static final int NEW=0;
    static final int STARTING=1;
    static final int RUNNING=2;
    static final int FINISHED=3;

    //vars
    protected Intent sockIntent;
    int running=NEW;

    //the log
    ArrayList<ArrayList<String>> recvLog=new ArrayList<ArrayList<String> >();
    ArrayList<Pair<ArrayList<String>, Boolean>> writeLog=new ArrayList<Pair<ArrayList<String>, Boolean> >();

    boolean serviceLogin(ArrayList<String> data)
    {
        String command=data.get(0);

        //did the user successfully log in?
        boolean success=command.equals("S");
        if(success)
        {
            if(data.size() < 6)
            {
                stopGService();
                return false;
            }

            //sid
            Gattic.global.sid=data.get(1);
            //id
            Gattic.global.id=Long.parseLong(data.get(2));
            //number
            Gattic.global.number=data.get(3);
            //name
            String newName=data.get(4);
            //terms of service
            Gattic.global.TOS=data.get(5).equals("1");

            //save the data
            Gattic.global.saveData();

            //now if we need to know something about ourselfs, just check the user with the same id
            Account account=new Account(Gattic.global.id, Gattic.global.number, newName);
            Gattic.global.addAccount(account);
        }
        else//didnt login successfully
        {
            String text="";
            if(command.equals("E"))
            {
                try
                {
                    int err=Integer.parseInt(data.get(1));
                    switch(err)
                    {
                        case 1:
                            text="Login information does not match";
                            break;
                        case 3:
                            text="Session expired";
                            break;
                        case 2: case 4://dont want to tell the user what happened here
                            text="Server Error";
                        break;
                        case 7:
                            text="Update required!";
                            break;
                        default://dont know what happened here
                            text="Unknown Error";
                            break;
                    }
                }
                catch(NumberFormatException ex)
                {
                    text="Unknown Error";//dont know what happened here
                }
            }
            else text="Unknown Error";//dont know what happened here

            //reconnection layout: "text"?
        }

        return success;
    }

    public GatticService()
    {
        //service instance
        super();
        Gattic.global.gService=this;
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return Gattic.global.mMessengerMain.getBinder();
    }

    @Override
    public boolean onUnbind(Intent intent)
    {
        return false;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        super.onStartCommand(intent, flags, startId);

        //the gService
        if(Gattic.global.gService == null)
        {
            recvLog.clear();
            writeLog.clear();
            Gattic.global.gService=this;
        }

        //did we start the fire
        sockIntent=intent;

        if(running == RUNNING)
            stopGService();

        //dont want multiple threads
        if((running == NEW) || (running == FINISHED))
        {
            //want the start sticky to return
            Runnable netRun=new Runnable()
            {
                public void run()
                {
                    networkServiceHelper();
                }
            };
            Thread networkThread=new Thread(netRun);
            networkThread.setName("networkThread");
            networkThread.start();
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        Gattic.global.socks.closeSocket(Sockets.CLOSED);
    }

    static void connect(ArrayList<String> wData, final int connectionType)
    {
        if(!Gattic.global.isService)
        {
            Context context=Gattic.getGatticContext();

            Intent intent=new Intent(context, GatticService.class);
            intent.putExtra("Connection", connectionType);
            intent.putStringArrayListExtra("wData", wData);

            context.startService(intent);
            context.bindService(intent, Gattic.global.mConnection, Context.BIND_AUTO_CREATE);
        }
    }

    void stopGService()
    {
        //stop the service
        stopSelf();

        //set the flag
        running=FINISHED;
    }

    private void networkServiceHelper()
    {
        running=STARTING;
        Gattic.global.isService=true;

        int connectionType;
        ArrayList<String> wData=new ArrayList<String>();

        //thanks to START_STICKY, reconnecting
        if(sockIntent == null)
        {
            if(Gattic.global.sid.isEmpty()) return;

            connectionType=Sockets.LOGIN;
            wData.add(Gattic.global.sid);
            wData.add(Data.VERSION);
            wData.add("L");

            //Reconnection text if the client is open
            if(Gattic.global.isIPCBound())
            {
                ArrayList<String> wData2=new ArrayList<String>();
                wData2.add("Reconnecting...");
                wData2.add("1");
                wData2.add("1");
                IPC.writeIPC(wData2, IPC.RECONNECT_LAYOUT, IPC.UI);
            }
        }
        else//we called it
        {
            connectionType=sockIntent.getIntExtra("Connection", 0);
            wData.addAll(sockIntent.getStringArrayListExtra("wData"));

            //turns out we do need this
            while(!Gattic.global.isIPCBound())
            {
                try { Thread.sleep(10); } catch(InterruptedException ex) { break; }
            }
        }

        //the response structure
        ArrayList<ArrayList<String> > dataList=new ArrayList<ArrayList<String> >();

        //Open the connection
        if(Gattic.global.socks.openSocket())
        {
            //we connected
            Gattic.global.socks.status=Sockets.CONNECTED;

            //tell the UI whats up
            ArrayList<String> wData2=new ArrayList<String>();
            wData2.add(Integer.toString(Gattic.global.socks.status));
            IPC.writeIPC(wData2, IPC.CLIENT_SOCK_UPDATE, IPC.UI);

            //tell the server whats up
            if(Gattic.global.socks.write(wData, 420L))
                dataList.addAll(Gattic.global.socks.read(420L));
        }

        //login/register/reconnect
        ArrayList<String> data=(dataList.size() > 0)? dataList.get(0):new ArrayList<String>();
        boolean success=false;

        //no response from server?
        if(data.size() == 0)
        {
            switch(connectionType)
            {
                case Sockets.LOGIN: IPC.writeIPC(data, IPC.LOGINACT_FAIL, IPC.UI); break;
                case Sockets.REGISTER: IPC.writeIPC(data, IPC.REGACT_FAIL, IPC.UI); break;
                case Sockets.QUICKLOGIN: IPC.writeIPC(data, IPC.QUICKLOGIN_FAIL, IPC.UI); break;
                case Sockets.RECONNECT: IPC.writeIPC(data, IPC.MAINACT_FAIL, IPC.UI); break;
            }
        }
        else
        {
            //response from server
            success=serviceLogin(data);

            //so the UI knows what to do
            if(success)
                recvLog.add(data);

            //UI login
            switch(connectionType)
            {
                case Sockets.LOGIN: IPC.writeIPC(data, IPC.LOGINACT_SUCCESS, IPC.UI); break;
                case Sockets.REGISTER: IPC.writeIPC(data, IPC.REGACT_SUCCESS, IPC.UI); break;
                case Sockets.QUICKLOGIN: IPC.writeIPC(data, IPC.QUICKLOGIN_SUCCESS, IPC.UI); break;
                case Sockets.RECONNECT: IPC.writeIPC(data, IPC.MAINACT_SUCCESS, IPC.UI); break;
            }
        }

        //dont need this anymore
        sockIntent=null;

        //lets do it
        if(success)
        {
            running=RUNNING;
            //listen carefully...
            Runnable readRun=new Runnable()
            {
                public void run()
                {
                    Gattic.global.socks.readEngine();
                }
            };
            Thread readThread=new Thread(readRun);
            readThread.setName("readThread");
            readThread.start();

            //stuff to write
            for(int i=0;i<Gattic.global.gService.writeLog.size();++i)
            {
                Pair<ArrayList<String>, Boolean> pair=Gattic.global.gService.writeLog.get(i);
                if((pair != null) && (pair.first != null) && (pair.second != null) && (!pair.second))
                    Sockets.writeEngine(pair.first);
            }

            //dont want to rewrite (ex: chat message duplicates) (implement)
            writeLog.clear();
        }
        else stopGService();
    }
}
