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

import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.ByteArrayInputStream
import org.bouncycastle.bcpg.ArmoredInputStream
import org.bouncycastle.bcpg.PublicKeyAlgorithmTags
import org.bouncycastle.openpgp.PGPPublicKeyRing
import org.bouncycastle.openpgp.PGPSecretKeyRing
import org.bouncycastle.openpgp.operator.bc.BcKeyFingerprintCalculator
import org.bouncycastle.openpgp.operator.bc.BcPBESecretKeyDecryptorBuilder
import org.bouncycastle.openpgp.operator.bc.BcPGPDigestCalculatorProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PgpKeyGeneratorInstrumentedTest {

    private val passphrase = "correct horse battery staple"

    @Test
    fun generates_legacy_v4_key_on_device_and_exports_for_interop() {
        val result = PgpKeyGenerator.generate(
            "Emu User",
            "emu@astermail.org",
            passphrase.toCharArray(),
        )

        val public_ring = ArmoredInputStream(
            ByteArrayInputStream(result.armored_public_key.toByteArray()),
        ).use { PGPPublicKeyRing(it, BcKeyFingerprintCalculator()) }

        val keys = public_ring.publicKeys.asSequence().toList()
        val master = keys.first { it.isMasterKey }
        val subkey = keys.first { !it.isMasterKey }

        assertEquals(4, master.version)
        assertEquals(PublicKeyAlgorithmTags.EDDSA_LEGACY, master.algorithm)
        assertEquals(4, subkey.version)
        assertEquals(PublicKeyAlgorithmTags.ECDH, subkey.algorithm)
        assertNotEquals(PublicKeyAlgorithmTags.Ed25519, master.algorithm)
        assertNotEquals(PublicKeyAlgorithmTags.X25519, subkey.algorithm)

        val secret_ring = ArmoredInputStream(
            ByteArrayInputStream(result.armored_private_key.toByteArray()),
        ).use { PGPSecretKeyRing(it, BcKeyFingerprintCalculator()) }

        val decryptor = BcPBESecretKeyDecryptorBuilder(BcPGPDigestCalculatorProvider())
            .build(passphrase.toCharArray())
        assertNotNull(secret_ring.secretKey.extractPrivateKey(decryptor))
    }
}
