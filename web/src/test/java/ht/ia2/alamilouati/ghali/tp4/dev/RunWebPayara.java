package ht.ia2.alamilouati.ghali.tp4.dev;

import java.io.IOException;

/** Lance le déploiement Payara local depuis une configuration Application d'IntelliJ. */
public final class RunWebPayara {
    private RunWebPayara() {
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        Process payara = new ProcessBuilder("cmd.exe", "/d", "/c", "run-web-payara.bat")
                .inheritIO()
                .start();
        System.exit(payara.waitFor());
    }
}
