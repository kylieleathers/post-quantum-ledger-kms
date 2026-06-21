ThisBuild / scalaVersion := "2.13.14" // match whatever Scala version your target Canton release is built against
ThisBuild / version := "0.1.0"

libraryDependencies ++= Seq(
  "com.github.pureconfig" %% "pureconfig" % "0.17.6",
  // Entrust nShield PKCS#11 access — via standard JCA/JCE, using Java's SunPKCS11
  // provider configured against Entrust's nCipherKM/PKCS11 library. 

lazy val root = (project in file("."))
  .settings(
    name := "canton-entrust-kms-driver",
    assembly / assemblyJarName := "canton-entrust-kms-driver.jar"
  )
