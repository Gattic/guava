package com.vox.meetup.gattic;

import java.util.Calendar;

class Crypt
{
    long[] eText;
    byte[] dText;

    int size=0;//size of block (in longs)
    int linesRead=0;
    boolean error=false;
    private long cTime=0L;
    private int shmea=0;
    private int brej=0;

    Crypt()
    {
        eText=null;
        dText=null;
        size=0;
        linesRead=0;
        cTime=0L;
        shmea=0;
        brej=0;
        error=false;
    }

    void encrypt(String text, long key)
    {
        //info
        char[] cText=text.toCharArray();
        size=text.length();
        dText=new byte[size];
        for(int i=0;i<size;++i) dText[i]=(byte)cText[i];

        ++size;//plus the key
        linesRead=size;
        cTime= Calendar.getInstance().getTimeInMillis()/1000L;

        String strTime=Long.toString(cTime);
        if(strTime.length() < 10)
        {
            error=true;
            return;
        }
        shmea=strTime.charAt(9)-0x30;
        shmea=(shmea==0)? 7:shmea;//lucky number 7
        brej=strTime.charAt(8)-0x30;
        brej=(brej==0)? 4:brej;//i also like 4

        //encrpyt
        eText=new long[size];
        long len=((long)size)*10000000000L;//10 zeros
        eText[0]=(cTime+len)*key;
        for(int i=1;i<size;++i)
        {
            eText[i]=dText[i-1];
            eText[i]*=(long)shmea;
            eText[i]+=(long)brej;
            eText[i]*=key;
        }
    }

    void decrypt(long[] src, long key)
    {
        //info
        eText=src;

        if(key == 0)
        {
            error=true;
            return;
        }

        //switch to little endian
        long firstLine=Data.switchEndian(eText[0])/key;
        size=(int)(firstLine/10000000000L);//10 zeros
        int linesToRead=(eText.length>size)? size:eText.length;
        if(linesToRead <= 0)
        {
            error=true;
            return;
        }

        cTime=firstLine-(((long)size)*10000000000L);

        String strTime=Long.toString(cTime);
        if(strTime.length() < 10)
        {
            error=true;
            return;
        }

        shmea=strTime.charAt(9)-0x30;
        shmea=(shmea==0)? 7:shmea;//lucky number 7
        brej=strTime.charAt(8)-0x30;
        brej=(brej==0)? 4:brej;//i also like 4

        //decrpyt
        dText=new byte[size];
        int i=1;
        for(;i<linesToRead;++i)
        {
            //switch to little endian
            long y=Data.switchEndian(eText[i]);
            y/=key;
            y-=(long)brej;
            y/=(long)shmea;
            dText[i-1]=(byte)y;
        }

        //how many lines did we decrypt
        linesRead=i;
    }

    @Override
    public String toString()
    {
        String text="";
        for(int i=0;i<size-1;++i)//minus the key
            text+=Character.toString((char)dText[i]);

        return text;
    }
}