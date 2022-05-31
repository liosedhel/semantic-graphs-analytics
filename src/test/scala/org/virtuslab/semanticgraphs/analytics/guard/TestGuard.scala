package org.virtuslab.semanticgraphs.analytics.guard

import org.junit.Test

class TestGuard {

  @Test def testGuard(): Unit = {
    val archGuard = CheckArchitecture.readNodes("/Users/kborowski/virtuslab/graphbuddy/archtestproject/scala-arch-test-project")

    archGuard.methods().inPackage("domain").methods.values.flatMap(_.edges).foreach(println)
    archGuard.methods().inPackage("domain").shouldNotCall(node => node.properties.get("package").forall(_.contains("infrastructure")))
  }

}
