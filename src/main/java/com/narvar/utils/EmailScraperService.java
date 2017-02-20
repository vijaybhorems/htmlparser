package com.narvar.utils;

import javax.mail.*;
import javax.mail.internet.ContentType;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.ParseException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

public class EmailScraperService {
    static String protocol;
    static String host = null;
    static String user = null;
    static String password = null;
    static String mbox = null;
    static String url = null;
    static int port = -1;
    static boolean verbose = false;
    static boolean debug = false;
    static boolean showStructure = false;
    static boolean showMessage = false;
    static boolean showAlert = false;
    static boolean saveAttachments = false;
    static int attnum = 1;
    private String htmlMessage;
    private String toEmail;
    private String subjectLine;
    private String bccEmail;

    public void parseContents(Part p) throws Exception {
        if (p instanceof Message)
            parseEnvelopeDetails((Message)p);

        String ct = p.getContentType();
        try {
            pr("CONTENT-TYPE: " + (new ContentType(ct)).toString());
        } catch (ParseException pex) {
            pr("BAD CONTENT-TYPE: " + ct);
        }
        String filename = p.getFileName();
        if (filename != null)
            pr("FILENAME: " + filename);

        /*
         * Using isMimeType to determine the content type avoids
         * fetching the actual content data until we need it.
         */
        if (p.isMimeType("text/plain")) {
            // NO OP
        } else if (p.isMimeType("multipart/*")) {
            pr("This is a Multipart");
            pr("---------------------------");
            Multipart mp = (Multipart)p.getContent();
            level++;
            int count = mp.getCount();
            for (int i = 0; i < count; i++)
                parseContents(mp.getBodyPart(i));
            level--;
        } else if (p.isMimeType("message/rfc822")) {
            pr("This is a Nested Message");
            pr("---------------------------");
            level++;
            parseContents((Part)p.getContent());
            level--;
        } else {
            if (!showStructure && !saveAttachments) {
                /*
                 * If we actually want to see the data, and it's not a
                 * MIME type we know, fetch it and check its Java type.
                 */
                Object o = p.getContent();
                if (o instanceof String) {
                    System.out.println("This is a string");
                    System.out.println("---------------------------");
                    String emailHtml = (String)o;
                    this.setHtmlMessage(emailHtml);
                } else if (o instanceof InputStream) {
                    pr("This is just an input stream");
                    pr("---------------------------");
                    InputStream is = (InputStream)o;
                    int c;
                    while ((c = is.read()) != -1)
                        System.out.write(c);
                } else {
                    pr("This is an unknown type");
                    pr("---------------------------");
                    pr(o.toString());
                }
            } else {
                // just a separator
                pr("---------------------------");
            }
        }

        /*
         * If we're saving attachments, write out anything that
         * looks like an attachment into an appropriately named
         * file.  Don't overwrite existing files to prevent
         * mistakes.
         */
        if (saveAttachments && level != 0 && p instanceof MimeBodyPart &&
                !p.isMimeType("multipart/*")) {
            String disp = p.getDisposition();
            // many mailers don't include a Content-Disposition
            if (disp == null || disp.equalsIgnoreCase(Part.ATTACHMENT)) {
                if (filename == null)
                    filename = "Attachment" + attnum++;
                pr("Saving attachment to file " + filename);
                try {
                    File f = new File(filename);
                    if (f.exists())
                        // XXX - could try a series of names
                        throw new IOException("file exists");
                    ((MimeBodyPart)p).saveFile(f);
                } catch (IOException ex) {
                    pr("Failed to save attachment: " + ex);
                }
                pr("---------------------------");
            }
        }
    }

    public void parseEnvelopeDetails(Message m) throws Exception {
        pr("This is the message envelope");
        pr("---------------------------");
        Address[] a;
        // FROM
        if ((a = m.getFrom()) != null) {
            for (int j = 0; j < a.length; j++)
                pr("FROM: " + a[j].toString());
        }

        // REPLY TO
        if ((a = m.getReplyTo()) != null) {
            for (int j = 0; j < a.length; j++)
                pr("REPLY TO: " + a[j].toString());
        }

        // BCC TO
        if ((a = m.getRecipients(Message.RecipientType.BCC)) != null) {
            for (int j = 0; j < a.length; j++) {
                pr("BCC TO: " + a[j].toString());
                this.setBccEmail(a[j].toString());
            }
        } else {
            String bccAddress = m.getHeader("Received")[1].substring(m.getHeader("Received")[1].indexOf("for <") + 5, m.getHeader("Received")[1].indexOf(">;"));
            this.setBccEmail(bccAddress);
        }

        // TO
        if ((a = m.getRecipients(Message.RecipientType.TO)) != null) {
            for (int j = 0; j < a.length; j++) {
                pr("TO: " + a[j].toString());
                this.setToEmail(a[j].toString());
                InternetAddress ia = (InternetAddress)a[j];
                if (ia.isGroup()) {
                    InternetAddress[] aa = ia.getGroup(false);
                    for (int k = 0; k < aa.length; k++)
                        pr("  GROUP: " + aa[k].toString());
                }
            }
        }

        // SUBJECT
        pr("SUBJECT: " + m.getSubject());
        this.setSubjectLine(m.getSubject());

        // DATE
        Date d = m.getSentDate();
        pr("SendDate: " +
                (d != null ? d.toString() : "UNKNOWN"));

        // FLAGS
        Flags flags = m.getFlags();
        StringBuffer sb = new StringBuffer();
        Flags.Flag[] sf = flags.getSystemFlags(); // get the system flags

        boolean first = true;
        for (int i = 0; i < sf.length; i++) {
            String s;
            Flags.Flag f = sf[i];
            if (f == Flags.Flag.ANSWERED)
                s = "\\Answered";
            else if (f == Flags.Flag.DELETED)
                s = "\\Deleted";
            else if (f == Flags.Flag.DRAFT)
                s = "\\Draft";
            else if (f == Flags.Flag.FLAGGED)
                s = "\\Flagged";
            else if (f == Flags.Flag.RECENT)
                s = "\\Recent";
            else if (f == Flags.Flag.SEEN)
                s = "\\Seen";
            else
                continue;	// skip it
            if (first)
                first = false;
            else
                sb.append(' ');
            sb.append(s);
        }

        String[] uf = flags.getUserFlags(); // get the user flag strings
        for (int i = 0; i < uf.length; i++) {
            if (first)
                first = false;
            else
                sb.append(' ');
            sb.append(uf[i]);
        }
        pr("FLAGS: " + sb.toString());

        // X-MAILER
        String[] hdrs = m.getHeader("X-Mailer");
        if (hdrs != null)
            pr("X-Mailer: " + hdrs[0]);
        else
            pr("X-Mailer NOT available");
    }

    static String indentStr = "                                               ";
    static int level = 0;

    /**
     * Print a, possibly indented, string.
     */
    public static void pr(String s) {
        if (showStructure)
            indentStr.substring(0, level * 2);
        System.out.println(s);
    }

    public String getHtmlMessage() {
        return htmlMessage;
    }

    public void setHtmlMessage(String htmlMessage) {
        this.htmlMessage = htmlMessage;
    }

    public String getToEmail() {
        return toEmail;
    }

    public void setToEmail(String toEmail) {
        this.toEmail = toEmail;
    }

    public String getSubjectLine() {
        return subjectLine;
    }

    public void setSubjectLine(String subjectLine) {
        this.subjectLine = subjectLine;
    }

    public String getBccEmail() {
        return bccEmail;
    }

    public void setBccEmail(String bccEmail) {
        this.bccEmail = bccEmail;
    }
}
