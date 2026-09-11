# Convert the ROM pk8/x509 keys into PKCS#12 keystores used by Gradle signingConfigs.
#
# The Gradle build signs debug/release with gradle-modules/prebuilts/keys/testkey.p12
# so Studio Run and `adb install -r` can replace the system Launcher. The p12 files
# are regenerated here because sync_from_rom.ps1 replaces the whole prebuilts/ tree.

$ErrorActionPreference = "Stop"
$tools = Split-Path -Parent $MyInvocation.MyCommand.Path
$moduleRoot = Split-Path -Parent $tools
$keys = Join-Path $moduleRoot "prebuilts\keys"

$jdk = & {
    . (Join-Path $PSScriptRoot "find-jdk.ps1")
    Get-Jdk21
}

$work = Join-Path $env:TEMP "launcher_make_keystore"
if (Test-Path $work) { Remove-Item -Recurse -Force $work }
New-Item -ItemType Directory -Force -Path $work | Out-Null

Set-Content -LiteralPath "$work\MakeP12.java" -Encoding ASCII -Value @'
import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;

public class MakeP12 {
    public static void main(String[] args) throws Exception {
        byte[] keyBytes = Files.readAllBytes(Paths.get(args[0]));
        PrivateKey key = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
        X509Certificate cert;
        try (InputStream in = new FileInputStream(args[1])) {
            cert = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(in);
        }
        KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(null, null);
        char[] pass = args[3].toCharArray();
        ks.setKeyEntry(args[4], key, pass, new java.security.cert.Certificate[]{cert});
        try (OutputStream out = new FileOutputStream(args[2])) {
            ks.store(out, pass);
        }
        System.out.println("OK " + args[2] + " alias=" + args[4]);
    }
}
'@

foreach ($name in @("testkey", "platform")) {
    $pk8 = Join-Path $keys "$name.pk8"
    $pem = Join-Path $keys "$name.x509.pem"
    $p12 = Join-Path $keys "$name.p12"
    if ((Test-Path $pk8) -and (Test-Path $pem)) {
        & (Join-Path $jdk "bin\java.exe") "$work\MakeP12.java" $pk8 $pem $p12 "android" $name
    } else {
        Write-Host "skip $name (pk8/x509 not found)"
    }
}
