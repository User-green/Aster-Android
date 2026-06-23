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

use argon2::{Algorithm, Argon2, Params, Version};
use std::slice;
use zeroize::Zeroize;

#[repr(C)]
pub struct AsterByteBuffer {
    pub data: *mut u8,
    pub len: usize,
    pub cap: usize,
}

impl AsterByteBuffer {
    fn from_vec(mut v: Vec<u8>) -> Self {
        let out = AsterByteBuffer {
            data: v.as_mut_ptr(),
            len: v.len(),
            cap: v.capacity(),
        };
        std::mem::forget(v);
        out
    }

    fn empty() -> Self {
        AsterByteBuffer {
            data: std::ptr::null_mut(),
            len: 0,
            cap: 0,
        }
    }
}

#[no_mangle]
pub extern "C" fn aster_buffer_free(buf: AsterByteBuffer) {
    if !buf.data.is_null() {
        unsafe {
            let mut v = Vec::from_raw_parts(buf.data, buf.len, buf.cap);
            v.zeroize();
            drop(v);
        }
    }
}

#[no_mangle]
pub extern "C" fn aster_secret_buffer_free(buf: AsterByteBuffer) {
    if !buf.data.is_null() {
        unsafe {
            let mut v = Vec::from_raw_parts(buf.data, buf.len, buf.cap);
            v.zeroize();
            drop(v);
        }
    }
}

const ARGON2_MIN_ITERATIONS: u32 = 3;
const ARGON2_MAX_ITERATIONS: u32 = 100;
const ARGON2_MEMORY_KIB: u32 = 64 * 1024;

#[no_mangle]
pub unsafe extern "C" fn aster_derive_password_hash(
    password: *const u8,
    password_len: usize,
    salt: *const u8,
    salt_len: usize,
    iterations: u32,
) -> AsterByteBuffer {
    if password.is_null() || salt.is_null() {
        return AsterByteBuffer::empty();
    }
    if iterations < ARGON2_MIN_ITERATIONS || iterations > ARGON2_MAX_ITERATIONS {
        return AsterByteBuffer::empty();
    }
    if password_len == 0 || salt_len < 8 {
        return AsterByteBuffer::empty();
    }
    let password_slice = slice::from_raw_parts(password, password_len);
    let salt_slice = slice::from_raw_parts(salt, salt_len);

    match derive_password_hash_impl(password_slice, salt_slice, iterations) {
        Ok(bytes) => AsterByteBuffer::from_vec(bytes),
        Err(_) => AsterByteBuffer::empty(),
    }
}

fn derive_password_hash_impl(
    password: &[u8],
    salt: &[u8],
    iterations: u32,
) -> Result<Vec<u8>, argon2::Error> {
    let params = Params::new(ARGON2_MEMORY_KIB, iterations, 1, Some(32))?;
    let argon2 = Argon2::new(Algorithm::Argon2id, Version::V0x13, params);
    let mut out = vec![0u8; 32];
    argon2.hash_password_into(password, salt, &mut out)?;
    Ok(out)
}
