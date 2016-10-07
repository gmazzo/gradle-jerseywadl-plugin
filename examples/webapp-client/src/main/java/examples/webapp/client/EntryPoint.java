package examples.webapp.client;

public class EntryPoint {

    public static void main(String args[]) {
        WepAppClient.Helloworld helloworld = WepAppClient.helloworld();

        System.out.print(helloworld.getAsTextPlain(String.class));
    }

}
