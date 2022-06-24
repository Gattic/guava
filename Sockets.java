package com.vox.meetup.gattic;

import android.support.v4.util.Pair;
import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;

public class Sockets
{
    //for the open
    static final int LOGIN=0;
    static final int REGISTER=1;
    static final int QUICKLOGIN=2;
    static final int RECONNECT=3;

    //for the status
    static final int CONNECTED=0;
    static final int CLOSED=1;
    static final int DISCONNECTED=2;
    static final int FAILED=3;
    static final int NEW=4;

    //general
    static final String serverAddress="69.126.139.205";//34.196.119.195
    private static final int mainPort=45019;

    //the variables
    private Socket sock;
    private DataOutputStream out;
    private DataInputStream in;
    private long[] overflow=new long[0];//for the in
    int status=NEW;

    private static boolean isEscaped(final int index, final String text)
    {
        int counter=0;
        while((index > 0) && (text.charAt(index-counter-1) == '%'))
            ++counter;
        return (counter%2 != 0);
    }

    private static int getNonEscapedDelimiter(final String text, final String delimiter)
    {
        int breakPoint=-1;
        do
        {
            if(breakPoint == -1) breakPoint=text.indexOf(delimiter);
            else breakPoint=text.indexOf(delimiter, breakPoint+delimiter.length());
        } while(isEscaped(breakPoint, text));
        return breakPoint;
    }

    private static ArrayList<String> Packet2Data(String text)
    {
        ArrayList<String> data=new ArrayList<String>();
        int breakPoint=getNonEscapedDelimiter(text, "|");

        do
        {
            int breakPoint2=getNonEscapedDelimiter(text, "\\|");

            if((breakPoint == -1) || (breakPoint2 == -1))
                break;

            if((breakPoint == 0) || (breakPoint2 == 0))
            {
                data.add("");

                int breakPoint3=(breakPoint==0)? breakPoint:breakPoint2;
                int bpLen=(breakPoint==0)? 1:2;
                int pieceLen=breakPoint3+bpLen;

                //substr the text
                text=text.substring(pieceLen);
            }
            else
            {
                if(breakPoint == breakPoint2+1)//reached the end
                {
                    data.add(text.substring(0, breakPoint2));
                    text=text.substring(breakPoint2+2);
                }
                else
                {
                    data.add(text.substring(0, breakPoint));
                    text=text.substring(breakPoint+1);
                }
            }

            breakPoint=getNonEscapedDelimiter(text, "|");

        } while(breakPoint >= 0);

        //escape
        for(int i=0;i<data.size();++i)
        {
            //unescape the characters
            String escText=data.get(i);

            for(int j=0;j<escText.length();++j)
            {
                if(escText.charAt(j) == '%')
                    escText=escText.substring(0, j)+escText.substring(j+1);
            }
            if(data.get(i).length() != escText.length()) data.set(i, escText);
        }

        return data;
    }

    private static String Data2Packet(ArrayList<String> data)
    {
        final String options="%\\|";

        String text="";
        for(int i=0;i<data.size();++i)
        {
            data.set(i, data.get(i).trim());

            //escape the characters
            String escText=data.get(i);
            for(int j=0;j<escText.length();++j)
            {
                int breakPoint=options.indexOf(escText.charAt(j));
                if(breakPoint != -1)
                {
                    escText=escText.substring(0, j)+"%"+escText.substring(j);
                    ++j;
                }
            }
            if(i < data.size()-1) text+=escText+"|";
            else text+=escText+"\\|";
        }

        return text;
    }

    boolean openSocket()
    {
        closeSocket(Sockets.CLOSED);

        try
        {
            sock=new Socket(serverAddress, mainPort);
            out=new DataOutputStream(sock.getOutputStream());
            in=new DataInputStream(sock.getInputStream());

            return true;
        }
        catch(IOException ex)
        {
            //could not connect to server
            closeSocket(FAILED);
            return false;
        }
    }

    private long[] reader()
    {
        ArrayList<Short> eBytes=new ArrayList<Short>();
        try
        {
            byte[] buf=new byte[1024];
            int bytesRead=0;

            if(in == null) return null;

            bytesRead=in.read(buf, 0, 1024);
            for(int i=0;i<bytesRead;++i)
                eBytes.add((short)((buf[i]<0)? buf[i]+256:buf[i]));

            //fail
            if(bytesRead == -1)
            {
                closeSocket(DISCONNECTED);
                return null;
            }

        }
        catch(IOException ex)
        {
            closeSocket(DISCONNECTED);
            return null;
        }

        //make it LONGer (lol)
        int tSize=eBytes.size()/8;
        long[] eText=new long[tSize];
        for(int i=0;i<tSize;++i)
        {
            eText[i]=(((long)eBytes.get(i*8))<<56L) |
                    (((long)eBytes.get((i*8)+1))<<48L) |
                    (((long)eBytes.get((i*8)+2))<<40L) |
                    (((long)eBytes.get((i*8)+3))<<32L) |
                    (((long)eBytes.get((i*8)+4))<<24L) |
                    (((long)eBytes.get((i*8)+5))<<16L) |
                    (((long)eBytes.get((i*8)+6))<<8L) |
                    ((long)eBytes.get((i*8)+7));
        }
        return eText;
    }

    ArrayList<ArrayList<String> > read(long key)
    {
        ArrayList<ArrayList<String> > dataList=readHelper(key);

        //remove the empty data vectors
        for(int i=0;i<dataList.size();++i)
        {
            ArrayList<String> data=dataList.get(i);
            /*for(int j=0;j<data.size();++j)
            {
                if(data.get(j).isEmpty())
                {
                    data.remove(j);
                    --j;
                }
            }*/

            //if its valid, add it to the log
            if(data.size() <= 0)
            {
                dataList.remove(i);
                --i;
            }
            else
                Gattic.global.gService.recvLog.add(new ArrayList<String>(dataList.get(i)));
        }

        return dataList;
    }

    private ArrayList<ArrayList<String> > readHelper(long key)
    {
        ArrayList<ArrayList<String> > dataList=new ArrayList<ArrayList<String> >();

        int balance=0;
        do
        {
            //get the things to read
            long[] eText=balance==1? null:reader();

            //error in reader, whatevz
            if(eText == null)
                eText=new long[0];

            //overflow+eText
            if(overflow.length > 0)
            {
                //overflow+eText
                long[] eText2=new long[overflow.length+eText.length];
                System.arraycopy(overflow, 0, eText2, 0, overflow.length);
                System.arraycopy(eText, 0, eText2, overflow.length, eText.length);

                //move it in
                eText=new long[eText2.length];
                System.arraycopy(eText2, 0, eText, 0, eText2.length);

                //reset this
                overflow=new long[0];
            }

            if(eText.length == 0)
                return dataList;

            //we good, decrypt
            Crypt crypt=new Crypt();
            crypt.decrypt(eText, key);

            //error in crypt
            if(crypt.error)
                return dataList;

            //starving
            if(crypt.linesRead < crypt.size)
            {
                balance=-1;

                //overflow
                overflow=new long[crypt.linesRead];
                System.arraycopy(eText, 0, overflow, 0, crypt.linesRead);
            }
            else if(crypt.linesRead == crypt.size)//take a bite
            {
                //set the text from the crypt object
                String text=crypt.toString();
                //Log.d("SOCKS", text.toString());

                //add the data
                if(!text.isEmpty())
                    dataList.add(Packet2Data(text));

                //perfect
                if(eText.length == crypt.size)
                    balance=0;
                else if(eText.length > crypt.size)//wrap the leftovers
                {
                    if(Gattic.global.gService.running == GatticService.STARTING)
                        balance=0;
                    else
                        balance=1;

                    //overflow
                    overflow=new long[eText.length-crypt.size];
                    System.arraycopy(eText, crypt.size, overflow, 0, eText.length-crypt.size);
                }
            }

        } while(balance != 0);

        return dataList;
    }

    void readEngine()//only the service should execute this
    {
        while((sock != null) && (in != null) && (status == CONNECTED))
        {
            ArrayList<ArrayList<String> > dataList=read(Gattic.global.id);
            for(int dl=0;dl<dataList.size();++dl)
            {
                //get the data from the datalist
                ArrayList<String> data=dataList.get(dl);//because of data being passed by reference,
                ArrayList<String> data2=new ArrayList<>(data);//even though java doesnt pass by reference
                if((data.size() == 0) || (data2.size() == 0)) break;//connection closed or disconnected

                //do our thing
                Command.execute(data);

                //write to the ui
                IPC.writeIPC(data2, IPC.SERVICE2UI, IPC.UI);
            }
        }
    }

    boolean write(ArrayList<String> wData, long key)
    {
        if((sock == null) || (out == null) || (status != CONNECTED)) return false;

        String text=Data2Packet(wData);

        if(text.equals("")) return false;

        try
        {
            //encrypt
            Crypt crypt=new Crypt();
            crypt.encrypt(text, key);
            byte[] buf=new byte[crypt.size*8];//8 bytes in a long

            for(int i=0;i<crypt.size;++i)
            {
                ByteBuffer buffer=ByteBuffer.allocate(Long.SIZE/8);//8 bits in a byte
                buffer.putLong(crypt.eText[i]);
                byte[] tempBuf=buffer.array();

                System.arraycopy(tempBuf, 0, buf, i*8, tempBuf.length);
            }

            //sometimes the session wont delete, but whatevz
            if(out != null) out.write(buf, 0, buf.length);
            else return false;

            return true;
        }
        catch(IOException ex)
        {
            closeSocket(DISCONNECTED);
            return false;
        }
    }

    static void writeEngine(final ArrayList<String> wData)
    {
        Runnable writeRun=new Runnable()
        {
            public void run()
            {
                if(Gattic.global.isService)
                {
                    //writeLog
                    if(Gattic.global.socks.write(wData, Gattic.global.id))
                    {
                        //log it
                        Gattic.global.gService.writeLog.add(new Pair<ArrayList<String>, Boolean>(wData, true));
                    }
                    else
                    {
                        //will be useful for pending writes when there is no connection
                        Gattic.global.gService.writeLog.add(new Pair<ArrayList<String>, Boolean>(wData, false));
                        Gattic.global.socks.closeSocket(DISCONNECTED);
                    }
                }
                else
                {
                    //write to the service
                    wData.add(0, Gattic.global.sid);//add the sid
                    IPC.writeIPC(wData, IPC.UI2SERVICE, IPC.SOCKETSHMOCKETS);
                }
            }
        };
        Thread writerThread=new Thread(writeRun);
        writerThread.start();
    }

    void closeSocket(int newStatus)
    {
        //set the new status BUT:
        //we dont want to override a closed status with failed
        if(status != CLOSED)
            status=newStatus;

        ArrayList<String> wData=new ArrayList<String>();
        wData.add(Integer.toString(status));
        IPC.writeIPC(wData, IPC.CLIENT_SOCK_UPDATE, IPC.UI);

        if(sock != null)
        {
            try { sock.close(); } catch(Exception ignored) { }
            try { sock.shutdownInput(); } catch(Exception ignored) { }
            try { sock.shutdownOutput(); } catch(Exception ignored) { }
            sock=null;
        }
        if(in != null)
        {
            try { in.close(); } catch(IOException ignored) { }
            in=null;
        }
        if(out != null)
        {
            try { out.close(); } catch(IOException ignored) { }
            out=null;
        }

        //reconnect a few times
        Log.d("SOCKS", "CLOSE1: "+(Gattic.global.isService));
        Log.d("SOCKS", "CLOSE2: "+(status));
        if((Gattic.global.isService) && (status == DISCONNECTED))
        {
            Gattic.global.gService.stopGService();

            ArrayList<String> wData2=new ArrayList<String>();
            wData2.add(Gattic.global.sid);
            wData2.add(Data.VERSION);
            wData2.add("C");
            IPC.writeIPC(wData2, IPC.RECONNECT, IPC.UI);
        }
    }
}