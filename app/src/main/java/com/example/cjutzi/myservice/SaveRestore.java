package com.example.cjutzi.myservice;

import android.util.Log;

import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;

/**
 * Created by cjutzi on 11/16/17.
 */

public class SaveRestore
{
    static String  DEBUG_TAG              = "SaveRestore";

    /**
     *
     * @param externalFileDir
     * @param fileName
     * @return
     */
    static final HashMap<String,Object> restoreStuff(String externalFileDir, String fileName)
    {
        HashMap<String,Object> hashList = new HashMap<String, Object>();


        if (!externalFileDir.endsWith("/"))
            externalFileDir+="/";

        if (externalFileDir != null)
        {
            try
            {
                FileInputStream fis = new FileInputStream(externalFileDir+fileName);
                ObjectInputStream ois = new ObjectInputStream(fis);
                hashList = (HashMap) ois.readObject();
                ois.close();
                fis.close();
            }
            catch (RuntimeException re)
            {
                Log.e(DEBUG_TAG,re.getMessage());
            }
            catch (FileNotFoundException e)
            {
                Log.e(DEBUG_TAG,"restoreStuff() : File not Ffound - "+externalFileDir+fileName);
            }
            catch ( EOFException e)
            {
                Log.e(DEBUG_TAG,"restoreStuff() : EOF Exception");
            }
            catch (IOException e)
            {
                 Log.e(DEBUG_TAG,e.getMessage());
            }
            catch (ClassNotFoundException e)
            {
                Log.e(DEBUG_TAG,e.getMessage());
            }
            catch (Exception e)
            {
                Log.e(DEBUG_TAG,e.getMessage());
            }
        }
        return hashList;
    }

    /**
     *
     * @param externalFileDir
     * @param hashToSave
     * @param fileName
     */
    static final void saveStuff(String externalFileDir, HashMap<String,Object> hashToSave, String fileName)
    {
        if (!externalFileDir.endsWith("/"))
            externalFileDir+="/";

        if (externalFileDir != null)
        {
            try
            {
                FileOutputStream fos= new FileOutputStream(externalFileDir+fileName);
                ObjectOutputStream oos= new ObjectOutputStream(fos);
                oos.writeObject(hashToSave);
                oos.close();
                fos.close();
            }
            catch (FileNotFoundException e)
            {
                Log.e(DEBUG_TAG,"restoreStuff() : File not Found - "+externalFileDir+fileName);
            }
            catch ( EOFException e)
            {
                Log.e(DEBUG_TAG,"restoreStuff() : EOF Exception");
            }
            catch (IOException e)
            {
                Log.e(DEBUG_TAG,e.getMessage());
            }
        }
    }

}
