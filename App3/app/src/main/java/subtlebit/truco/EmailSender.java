package subtlebit.truco;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.AccessController;
import java.security.Provider;
import java.security.Security;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

public class EmailSender extends javax.mail.Authenticator {

    private String mailhost = "smtp.gmail.com";
    private String user;
    private String password;
    private Session session;

    static {
        Security.addProvider(new JSSEProvider());
    }

    public EmailSender(String user, String password) {
        this.user = user;
        this.password = password;

        Properties props = new Properties();
        props.setProperty("mail.transport.protocol", "smtp");
        props.setProperty("mail.host", mailhost);
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.port", "465");
        props.put("mail.smtp.socketFactory.port", "465");
        props.put("mail.smtp.socketFactory.class",
                "javax.net.ssl.SSLSocketFactory");
        props.put("mail.smtp.socketFactory.fallback", "false");
        props.setProperty("mail.smtp.quitwait", "false");

        session = Session.getDefaultInstance(props, this);
    }

    protected PasswordAuthentication getPasswordAuthentication() {
        return new PasswordAuthentication(user, password);
    }

    public synchronized void sendMail(String subject, String body, Bitmap image_attachment, String sender, String recipients) throws Exception {
        try {

            // Create the message and the multipart (the contents of the message)
            MimeMessage message = new MimeMessage(session);
            Multipart message_multipart = new MimeMultipart();
            DataHandler handler = new DataHandler(new ByteArrayDataSource(
                    body.getBytes(), "text/plain"));
            message.setDataHandler(handler);

            // Set sender and recipient
            message.setSender(new InternetAddress(sender));
            message.setSubject(subject);

            // Set content
            BodyPart messageBodyPart = new MimeBodyPart();
            messageBodyPart.setText(body);
            message_multipart.addBodyPart(messageBodyPart);

            // Set attachment
            if(image_attachment!=null){
                MimeBodyPart messageAttachmentPart = new MimeBodyPart();

                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                image_attachment.compress(Bitmap.CompressFormat.PNG,0,stream);
                byte[] compressed_picture_array = stream.toByteArray();
                messageAttachmentPart.setDataHandler(new DataHandler(new ByteArrayDataSource(compressed_picture_array)));
                messageAttachmentPart.setFileName("image.png");
                message_multipart.addBodyPart(messageAttachmentPart);
            }

            // Put content and attachment in message_multipart
            message.setContent(message_multipart);

            // Parse recipients and send
            if (recipients.indexOf(',') > 0)
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipients));
            else
                message.setRecipient(Message.RecipientType.TO, new InternetAddress(recipients));
            Transport.send(message);

        } catch (Exception ex) {
            Log.i("ERROR",ex.getMessage().toString());
        }
    }

    public class ByteArrayDataSource implements DataSource {
        private byte[] data;
        private String type;

        public ByteArrayDataSource(byte[] data, String type) {
            super();
            this.data = data;
            this.type = type;
        }

        public ByteArrayDataSource(byte[] data) {
            super();
            this.data = data;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getContentType() {
            if (type == null)
                return "application/octet-stream";
            else
                return type;
        }

        public InputStream getInputStream() throws IOException {
            return new ByteArrayInputStream(data);
        }

        public String getName() {
            return "ByteArrayDataSource";
        }

        public OutputStream getOutputStream() throws IOException {
            throw new IOException("Not Supported");
        }
    }
}

final class JSSEProvider extends Provider {
    public JSSEProvider() {
        super("HarmonyJSSE", 1.0, "Harmony JSSE Provider");
        AccessController.doPrivileged(new java.security.PrivilegedAction<Void>() {public Void run() {
            put("SSLContext.TLS", "org.apache.harmony.xnet.provider.jsse.SSLContextImpl");
            put("Alg.Alias.SSLContext.TLSv1", "TLS");
            put("KeyManagerFactory.X509","org.apache.harmony.xnet.provider.jsse.KeyManagerFactoryImpl");
            put("TrustManagerFactory.X509", "org.apache.harmony.xnet.provider.jsse.TrustManagerFactoryImpl");
            return null;
        }
        });
    }
}

class SendEmail extends AsyncTask<Void, Void, Void> {

    String subject = "";
    String body = "";
    Context context;
    Bitmap bitmap;

    SendEmail(String Subject, String Body, Bitmap BitmapAttch, Context cont){
        subject = Subject;
        body = Body;
        context = cont;
        bitmap = BitmapAttch;
    }

    @Override
    protected Void doInBackground(Void...params){
        EmailSender sender = new EmailSender("sbt001gd@gmail.com", "QpqeXXYoGsowrdm52URWJ4N2IqVY2Gs0CLKLMLRkc2RP3GVR4zqLcxAgFGl3JFVJ");
        try {
            sender.sendMail(subject, body, bitmap,"sbt001gd@gmail.com", "sbt001gd@gmail.com");
        } catch (Exception ex) {
            Log.i("ERROR", ex.getMessage().toString());
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void v){
        Log.i("MSG","IS SENT?");
        Toast.makeText(context, "Message sent. Thanks for your feedback!", Toast.LENGTH_SHORT).show();
        return;
    }
}