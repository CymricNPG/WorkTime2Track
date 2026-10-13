package net.npg.wt2t.testing

internal val TEST_SBOM_BYTES = """
    {
      "bomFormat": "CycloneDX",
      "specVersion": "1.6",
      "version": 1,
      "components": [
        {
          "type": "library",
          "bom-ref": "pkg:maven/example/library@1.0",
          "name": "library",
          "version": "1.0",
          "licenses": [{"license": {"id": "Apache-2.0"}}],
          "purl": "pkg:maven/example/library@1.0"
        }
      ],
      "dependencies": []
    }
""".trimIndent().encodeToByteArray()
