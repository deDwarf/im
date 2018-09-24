package shared;

import org.testng.Assert;
import org.testng.annotations.Test;
import shared.msg.ChatMessage;
import shared.msg.LoginRequestMessage;
import shared.msg.Message;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class MessageTest {

    String WITH_TO_CHAR_MSG_EXPECTED = "YECA/0.1 Chat message\n" +
            "\n" +
            "{\n" +
            "  \"from\": \"Egor\",\n" +
            "  \"to\": \"Vitalina\",\n" +
            "  \"messageBody\": \"Hi there!\"\n" +
            "}";
    String WITHOUT_TO_CHAR_MSG_EXPECTED = "YECA/0.1 Chat message\n" +
            "\n" +
            "{\n" +
            "  \"from\": \"Egor\",\n" +
            "  \"messageBody\": \"Hi there!\"\n" +
            "}";


    @Test
    void constructMessageString(){
        Message msg = new ChatMessage("Egor", "Vitalina", "Hi there!");
        String str = msg.constructMessageString();
        System.out.println(str);

        Message msg1 = new ChatMessage("Egor", "Hi there!");
        String str1 = msg1.constructMessageString();
        System.out.println(str1);

        Message msg2 = new LoginRequestMessage("username", "password");
        String str2 = msg2.constructMessageString();
        System.out.println(str2);
    }

    @Test
    void tryParseTest(){
        Message msg = new ChatMessage("Egor", "Vitalina", "Hi there!");
        String str = msg.constructMessageString();
        Assert.assertTrue(Message.tryParse(str) instanceof ChatMessage);

        Message msg1 = new LoginRequestMessage("egor", "pwddd");
        String str1 = msg1.constructMessageString();
        Assert.assertTrue(Message.tryParse(str1) instanceof LoginRequestMessage);
    }

    @Test
    void test(){
        String hostname = "Unknown";
        InetAddress address = null;
        try
        {
            InetAddress addr;
            addr = InetAddress.getLocalHost();
            hostname = addr.getHostName();
            address = InetAddress.getByName("WINDOWS-21GELVO");
        }
        catch (UnknownHostException ex)
        {
            System.out.println("Hostname can not be resolved");
        }
        System.out.println(hostname);
        System.out.println(address.toString());
    }
}
