package com.example.josip.rpiandroidbt;

import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class MailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mail);

    }

    public void sendMail(View view) {
        EditText text_subject = (EditText) findViewById(R.id.text_subject);
        EditText text_message = (EditText) findViewById(R.id.text_message);
        Log.i("Send email", "");

        String[] TO = {"nikolicjosip95@gmail.com"};
        Intent emailIntent = new Intent(Intent.ACTION_SEND);
        emailIntent.setData(Uri.parse("mailto:"));
        emailIntent.setType("text/plain");


        emailIntent.putExtra(Intent.EXTRA_EMAIL, TO);
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, text_subject.getText().toString());
        emailIntent.putExtra(Intent.EXTRA_TEXT,  text_message.getText().toString());

        try {
            startActivity(Intent.createChooser(emailIntent, "Send e-mail..."));
            finish();
            Log.i("Finished sending e-mail", "");
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(MailActivity.this,
                    "There is no email client installed.", Toast.LENGTH_SHORT).show();
        }
    }
}
