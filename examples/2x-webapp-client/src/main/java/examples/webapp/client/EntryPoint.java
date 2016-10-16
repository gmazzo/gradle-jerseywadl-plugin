package examples.webapp.client;

import examples._2x_webapp.client.WepApp2XClient;

public class EntryPoint {

    public static void main(String args[]) {
        WepApp2XClient.Helloworld helloworld = WepApp2XClient.helloworld();

        System.out.print(helloworld.getAsTextPlain(String.class));
    }

}
