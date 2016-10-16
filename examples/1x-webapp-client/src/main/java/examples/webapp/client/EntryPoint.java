package examples.webapp.client;

import examples._1x_webapp.client.WebAppClient;

public class EntryPoint {

    public static void main(String args[]) {
        WebAppClient.Helloworld helloworld = WebAppClient.helloworld();

        System.out.print(helloworld.getAsTextPlain(String.class));
    }

}
