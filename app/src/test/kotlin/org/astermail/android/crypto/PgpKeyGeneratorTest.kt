//
// Aster Communications Inc.
//
// Copyright (c) 2026 Aster Communications Inc.
//
// This file is part of this project.
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU Affero General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU Affero General Public License for more details.
//
// You should have received a copy of the GNU Affero General Public License
// along with this program. If not, see <https://www.gnu.org/licenses/>.
//

package org.astermail.android.crypto

import java.io.ByteArrayInputStream
import org.bouncycastle.bcpg.ArmoredInputStream
import org.bouncycastle.bcpg.PublicKeyAlgorithmTags
import org.bouncycastle.openpgp.PGPPublicKeyRing
import org.bouncycastle.openpgp.PGPSecretKeyRing
import org.bouncycastle.openpgp.operator.bc.BcKeyFingerprintCalculator
import org.bouncycastle.openpgp.operator.bc.BcPBESecretKeyDecryptorBuilder
import org.bouncycastle.openpgp.operator.bc.BcPGPDigestCalculatorProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PgpKeyGeneratorTest {

    private val passphrase = "correct horse battery staple"

    @Test
    fun generates_v4_ed25519_legacy_keypair_not_rfc9580() {
        val result = PgpKeyGenerator.generate(
            "Alice",
            "alice@astermail.org",
            passphrase.toCharArray(),
        )

        assertTrue(result.armored_public_key.contains("BEGIN PGP PUBLIC KEY BLOCK"))
        assertTrue(result.armored_private_key.contains("BEGIN PGP PRIVATE KEY BLOCK"))

        val public_ring = read_public_ring(result.armored_public_key)
        val keys = public_ring.publicKeys.asSequence().toList()
        val master = keys.first { it.isMasterKey }
        val subkey = keys.first { !it.isMasterKey }

        assertEquals(4, master.version)
        assertEquals(PublicKeyAlgorithmTags.EDDSA_LEGACY, master.algorithm)
        assertEquals(4, subkey.version)
        assertEquals(PublicKeyAlgorithmTags.ECDH, subkey.algorithm)

        assertNotEquals(PublicKeyAlgorithmTags.Ed25519, master.algorithm)
        assertNotEquals(PublicKeyAlgorithmTags.X25519, subkey.algorithm)
        assertNotEquals(PublicKeyAlgorithmTags.RSA_GENERAL, master.algorithm)
    }

    @Test
    fun secret_key_unlocks_with_passphrase() {
        val result = PgpKeyGenerator.generate(
            "Bob",
            "bob@astermail.org",
            passphrase.toCharArray(),
        )

        val secret_ring = read_secret_ring(result.armored_private_key)
        val decryptor = BcPBESecretKeyDecryptorBuilder(BcPGPDigestCalculatorProvider())
            .build(passphrase.toCharArray())
        val private_key = secret_ring.secretKey.extractPrivateKey(decryptor)

        assertNotNull(private_key)
    }

    private fun read_public_ring(armored: String): PGPPublicKeyRing =
        ArmoredInputStream(ByteArrayInputStream(armored.toByteArray())).use {
            PGPPublicKeyRing(it, BcKeyFingerprintCalculator())
        }

    private fun read_secret_ring(armored: String): PGPSecretKeyRing =
        ArmoredInputStream(ByteArrayInputStream(armored.toByteArray())).use {
            PGPSecretKeyRing(it, BcKeyFingerprintCalculator())
        }
}
