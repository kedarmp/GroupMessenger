package edu.buffalo.cse.cse486586.groupmessenger1;

import android.app.Activity;
import android.content.ContentValues;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 *
 * @author stevko
 *
 */
public class GroupMessengerActivity extends Activity {
    private EditText composeEditText;
    private TextView mainTextView;
    Uri providerUri;
    private String[] portNos = {"11108","11112","11116","11120","11124"};
    private int LISTEN_PORT = 10000;
    private final static String TAG = GroupMessengerActivity.class.toString();
    private ServerSocket serverSocket;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        //build uri
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority("edu.buffalo.cse.cse486586.groupmessenger1.provider");
        uriBuilder.scheme("content");
        providerUri = uriBuilder.build();

        //setup sockets..
        try {
            serverSocket = new ServerSocket(LISTEN_PORT);
            new AcceptMessages().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } catch (IOException e) {
            e.printStackTrace();
        }

        composeEditText = (EditText) findViewById(R.id.editText1);

        /*
         * TODO: Use the TextView to display your messages. Though there is no grading component
         * on how you display the messages, if you implement it, it'll make your debugging easier.
         */
        mainTextView = (TextView) findViewById(R.id.textView1);
        mainTextView.setMovementMethod(new ScrollingMovementMethod());
        
        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(mainTextView, getContentResolver()));
        findViewById(R.id.button4).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        String message = composeEditText.getText().toString().trim();

                        if(message.isEmpty()) {
                            composeEditText.setError("Please enter a message!");
                            composeEditText.requestFocus();
                            return;
                        }

                        //call 5 asynctasks to send message
                        for(int i=0;i<portNos.length;i++) {
                            new SendMessage().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,message,portNos[i]);
                        }
//                        new SendMessage().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,message,"11112");
//                        new SendMessage().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,message,"11116");
                        composeEditText.setText("");
                    }
                });

    }


    private class SendMessage extends AsyncTask<String,Void,Void> {

        @Override
        protected Void doInBackground(String... strings) {
            String message = strings[0] + "\n";
            int portNo = Integer.valueOf(strings[1]);
            try {
                Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                        portNo);
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(),"UTF-8"));
                writer.write(message);
                writer.flush();
                //perform a read, and then close socket (this is a better way to close socket by "waiting for other end to close
                //the connection
                InputStreamReader r=new InputStreamReader(socket.getInputStream());
                if(r.read()==-1) {
                    writer.close();
                    r.close();
                    socket.close();
                }
//                writer.close();
//                socket.close();
                Log.d(TAG,"Message sent");
            } catch (UnknownHostException ex) {
                ex.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }


    }



    private class AcceptMessages extends AsyncTask<String,String,Void> {

        @Override
        protected Void doInBackground(String... strings) {
            String received = "";
            try {
                while (true) {
                    Socket client = serverSocket.accept();
                    Log.d(TAG,"Accepted connection");
                    BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream(),"UTF-8"));
                    received = reader.readLine();
                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(client.getOutputStream(),"UTF-8"));
                    writer.write("END");
                    writer.flush();

                    writer.close();
                    reader.close();
                    client.close();
                    Log.d(TAG,"Received: "+received);
                    publishProgress(received);

                    ContentValues value = new ContentValues();
                    value.put(GroupMessengerProvider.COLUMN_KEY, GroupMessengerProvider.FROM_REMOTE_AVD);
                    value.put(GroupMessengerProvider.COLUMN_VALUE, received);
                    getContentResolver().insert(providerUri, value); //TODO we're doing this asynchronously. Check this if any errors
                }
            }catch(IOException e){
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            //mainTextView.append("\n"+values[0]);    //i guess :/
            mainTextView.append(values[0] + "\n");
        }
    }

    @Override
    protected void onStop() {
        if(serverSocket!=null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }

}
