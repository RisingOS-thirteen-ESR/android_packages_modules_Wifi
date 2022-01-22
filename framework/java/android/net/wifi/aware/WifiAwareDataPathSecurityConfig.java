/*
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.net.wifi.aware;

import static android.net.wifi.aware.Characteristics.WIFI_AWARE_CIPHER_SUITE_NCS_PK_128;
import static android.net.wifi.aware.Characteristics.WIFI_AWARE_CIPHER_SUITE_NCS_PK_256;
import static android.net.wifi.aware.Characteristics.WIFI_AWARE_CIPHER_SUITE_NCS_SK_128;
import static android.net.wifi.aware.Characteristics.WIFI_AWARE_CIPHER_SUITE_NCS_SK_256;

import android.annotation.Nullable;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import java.util.Arrays;
import java.util.Objects;

/**
 * Wi-Fi Aware data-path security config. The config is used with
 * {@link WifiAwareNetworkSpecifier.Builder#setDataPathSecurityConfig(WifiAwareDataPathSecurityConfig)}
 * to request a secure data-path.
 */
public final class WifiAwareDataPathSecurityConfig implements Parcelable {
    private final byte[] mPmk;
    private final String mPassphrase;
    private final byte[] mPmkId;
    private final int mCipherSuite;

    /**
     * Generate a security config with necessary parameters. Use {@link #isValid()} to check before
     * calling
     * {@link WifiAwareNetworkSpecifier.Builder#setDataPathSecurityConfig(WifiAwareDataPathSecurityConfig)}
     * @param passphrase The passphrase to be used to encrypt the link.
     *                      See {@link Builder#setPskPassphrase(String)}
     * @param cipherSuite The cipher suite to be used to encrypt the link.
     *                    See {@link Builder#setCipherSuite(int)}
     * @param pmk A PMK (pairwise master key, see IEEE 802.11i) specifying the key to use for
     *            encrypting the data-path. See {@link Builder#setPmk(byte[])}
     * @param pmkId A PMKID (pairwise master key associated identifier, see IEEE 802.11) is
     *              generated by Diffie-Hellman key exchange together with a Pairwise Master Key
     *              (PMK), specifying the identifier associated to the key to use for encrypting
     *              the data-path. See {@link Builder#setPmkId(byte[])}
     * @hide
     */
    public WifiAwareDataPathSecurityConfig(@Characteristics.WifiAwareCipherSuites int cipherSuite,
            @Nullable byte[] pmk, @Nullable byte[] pmkId, @Nullable String passphrase) {
        mCipherSuite = cipherSuite;
        mPassphrase = passphrase;
        mPmk = pmk;
        mPmkId = pmkId;
    }

    private WifiAwareDataPathSecurityConfig(Parcel in) {
        mPmk = in.createByteArray();
        mPassphrase = in.readString();
        mPmkId = in.createByteArray();
        mCipherSuite = in.readInt();
    }

    public static final @NonNull Creator<WifiAwareDataPathSecurityConfig> CREATOR =
            new Creator<WifiAwareDataPathSecurityConfig>() {
                @Override
                public WifiAwareDataPathSecurityConfig createFromParcel(Parcel in) {
                    return new WifiAwareDataPathSecurityConfig(in);
                }

                @Override
                public WifiAwareDataPathSecurityConfig[] newArray(int size) {
                    return new WifiAwareDataPathSecurityConfig[size];
                }
            };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeByteArray(mPmk);
        dest.writeString(mPassphrase);
        dest.writeByteArray(mPmkId);
        dest.writeInt(mCipherSuite);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof WifiAwareDataPathSecurityConfig)) {
            return false;
        }

        WifiAwareDataPathSecurityConfig lhs = (WifiAwareDataPathSecurityConfig) obj;
        return mCipherSuite == lhs.mCipherSuite
                && Arrays.equals(mPmk, lhs.mPmk)
                && Objects.equals(mPassphrase, lhs.mPassphrase)
                && Arrays.equals(mPmkId, lhs.mPmkId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(Arrays.hashCode(mPmk), mPassphrase, Arrays.hashCode(mPmkId),
                mCipherSuite);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("WifiAwareDataPathSecurityConfig [");
        sb.append("cipherSuite=").append(mCipherSuite)
                .append(", passphrase=")
                .append((TextUtils.isEmpty(mPassphrase)) ? "<null>" : "<non-null>")
                .append(", PMK=")
                .append((mPmk == null) ? "<null>" : "<non-null>")
                .append(", PMKID=")
                .append((mPmkId == null) ? "<null>" : "<non-null>")
                .append("]");
        return sb.toString();
    }

    /**
     * Check if the security config is valid.
     * @see Builder#Builder(int)
     * @return True if it is valid, false otherwise.
     * @hide
     */
    public boolean isValid() {
        if (mCipherSuite == WIFI_AWARE_CIPHER_SUITE_NCS_SK_128
                || mCipherSuite == WIFI_AWARE_CIPHER_SUITE_NCS_SK_256) {
            if (TextUtils.isEmpty(mPassphrase) && mPmk == null) {
                return false;
            }
            if (!TextUtils.isEmpty(mPassphrase) && mPmk != null) {
                return false;
            }
            if (mPmkId != null) {
                return false;
            }
            if (WifiAwareUtils.validatePassphrase(mPassphrase) && mPmk == null) {
                return true;
            }
            return TextUtils.isEmpty(mPassphrase) && WifiAwareUtils.validatePmk(mPmk);
        } else if (mCipherSuite == WIFI_AWARE_CIPHER_SUITE_NCS_PK_128
                || mCipherSuite == WIFI_AWARE_CIPHER_SUITE_NCS_PK_256) {
            if (!WifiAwareUtils.validatePmk(mPmk) || !WifiAwareUtils.validatePmkId(mPmkId)) {
                return false;
            }
            return TextUtils.isEmpty(mPassphrase);
        }
        return false;
    }

    /**
     * Get the cipher suite specified in this config
     * @return one of {@code Characteristics#WIFI_AWARE_CIPHER_SUITE_*"}
     */
    public int getCipherSuite() {
        return mCipherSuite;
    }

    /**
     * Get the specified PMK in this config.
     * @see Builder#setPmk(byte[])
     * @return A PMK (pairwise master key, see IEEE 802.11i) specifying the key to use for
     * encrypting the data-path.
     */
    public @Nullable byte[] getPmk() {
        return mPmk;
    }

    /**
     * Get the specified PMKID in this config.
     * @return A PMKID (pairwise master key associated identifier, see IEEE 802.11) is generated
     * by Diffie-Hellman key exchange together with a Pairwise Master Key.
     */
    public @Nullable byte[] getPmkId() {
        return mPmkId;
    }

    /**
     * Get the specified passphrase in this config.
     * @return The passphrase to be used to encrypt the link.
     */
    public @Nullable String getPskPassphrase() {
        return mPassphrase;
    }

    /**
     * A builder class for a Wi-Fi Aware data-path security config to encrypt an Aware connection.
     */
    public static final class Builder {
        private  byte[] mPmk;
        private  String mPassphrase;
        private  byte[] mPmkId;
        private  int mCipherSuite;

        /**
         * Create a builder for a Wi-Fi Aware data-path security config to encrypt the link with
         * specified cipher suite. Use {@link Characteristics#getSupportedCipherSuites()} to get the
         * supported capabilities of the device.
         * <ul>
         * <li>For shared key cipher suite
         * {@link Characteristics#WIFI_AWARE_CIPHER_SUITE_NCS_SK_128} and
         * {@link Characteristics#WIFI_AWARE_CIPHER_SUITE_NCS_SK_256}, either passphrase or PMK must
         * be set.</li>
         * <li>For public key cipher suite
         * {@link Characteristics#WIFI_AWARE_CIPHER_SUITE_NCS_PK_128} and
         * {@link Characteristics#WIFI_AWARE_CIPHER_SUITE_NCS_PK_256}. Both PMK and PMKID must be
         * set.</li>
         * </ul>
         * @see WifiAwareNetworkSpecifier.Builder#setDataPathSecurityConfig(WifiAwareDataPathSecurityConfig)
         * @param cipherSuite The cipher suite to be used to encrypt the link. One of the
         *                    {@link Characteristics#WIFI_AWARE_CIPHER_SUITE_NCS_SK_128},
         *                    {@link Characteristics#WIFI_AWARE_CIPHER_SUITE_NCS_SK_256},
         *                    {@link Characteristics#WIFI_AWARE_CIPHER_SUITE_NCS_PK_128} and
         *                    {@link Characteristics#WIFI_AWARE_CIPHER_SUITE_NCS_PK_256}.
         */
        public Builder(@Characteristics.WifiAwareCipherSuites int cipherSuite) {
            if (cipherSuite != WIFI_AWARE_CIPHER_SUITE_NCS_SK_128
                    && cipherSuite != WIFI_AWARE_CIPHER_SUITE_NCS_SK_256
                    && cipherSuite != WIFI_AWARE_CIPHER_SUITE_NCS_PK_128
                    && cipherSuite != WIFI_AWARE_CIPHER_SUITE_NCS_PK_256) {
                throw new IllegalArgumentException("Invalid cipher suite");
            }
            mCipherSuite = cipherSuite;
        }

        /**
         * Configure the PSK Passphrase for the Wi-Fi Aware connection being requested. For shared
         * key cipher suite {@link Characteristics#WIFI_AWARE_CIPHER_SUITE_NCS_SK_128} and
         * {@link Characteristics#WIFI_AWARE_CIPHER_SUITE_NCS_SK_256}, either passphrase or PMK must
         * be set.
         *
         * @param pskPassphrase The passphrase to be used to encrypt the link. Alternatively, use
         *                      the {@link #setPmk(byte[])} to specify a PMK for shared key cipher
         *                      suite.
         * @return the current {@link Builder} builder, enabling chaining of builder methods.
         */
        public @NonNull Builder setPskPassphrase(@NonNull String pskPassphrase) {
            if (!WifiAwareUtils.validatePassphrase(pskPassphrase)) {
                throw new IllegalArgumentException("Passphrase must meet length requirements");
            }
            mPassphrase = pskPassphrase;
            return this;
        }

        /**
         * Configure the PMK for the Wi-Fi Aware connection being requested. For shared key cipher
         * suite {@link Characteristics#WIFI_AWARE_CIPHER_SUITE_NCS_SK_128} and
         * {@link Characteristics#WIFI_AWARE_CIPHER_SUITE_NCS_SK_256}, either passphrase or PMK must
         * be set.
         * For public key cipher suite {@link Characteristics#WIFI_AWARE_CIPHER_SUITE_NCS_PK_128}
         * and {@link Characteristics#WIFI_AWARE_CIPHER_SUITE_NCS_PK_256}. Both PMK and PMKID must
         * be set.
         *
         * @param pmk A PMK (pairwise master key, see IEEE 802.11i) specifying the key to use for
         *            encrypting the data-path. Alternatively, use the
         *            {@link #setPskPassphrase(String)} to specify a Passphrase instead for shared
         *            key cipher suite. Use the {@link #setPmkId(byte[])} together for public key
         *            cipher suite.
         * @return the current {@link Builder} builder, enabling chaining of builder
         *         methods.
         */
        public @NonNull Builder setPmk(@NonNull byte[] pmk) {
            if (!WifiAwareUtils.validatePmk(pmk)) {
                throw new IllegalArgumentException("PMK must 32 bytes");
            }
            mPmk = pmk;
            return this;
        }

        /**
         * Configure the PMKID for the Wi-Fi Aware connection being requested. For public key cipher
         * suite {@link Characteristics#WIFI_AWARE_CIPHER_SUITE_NCS_PK_128} and
         * {@link Characteristics#WIFI_AWARE_CIPHER_SUITE_NCS_PK_256}. both PMK and PMKID must set
         * {@link #setPmk(byte[])}
         *
         * @param pmkId A PMKID (pairwise master key associated identifier, see IEEE 802.11) is
         *              generated by Diffie-Hellman key exchange together with a Pairwise Master Key
         *              (PMK), specifying the identifier associated to the key to use for encrypting
         *              the data-path. Use the  {@link #setPmk(byte[])} together for public key
         *              cipher suite.
         * @return the current {@link Builder} builder, enabling chaining of builder
         *         methods.
         */
        public @NonNull Builder setPmkId(@NonNull byte[] pmkId) {
            if (!WifiAwareUtils.validatePmkId(pmkId)) {
                throw new IllegalArgumentException("PMKID must 16 bytes");
            }
            mPmkId = pmkId;
            return this;
        }

        /**
         * Create a {@link WifiAwareDataPathSecurityConfig} to set in
         * {@link WifiAwareNetworkSpecifier.Builder#setDataPathSecurityConfig(WifiAwareDataPathSecurityConfig)} to encrypt the link.
         * @return A {@link WifiAwareDataPathSecurityConfig} to be used for encrypting the Wi-Fi
         * Aware data-path.
         */
        public @NonNull WifiAwareDataPathSecurityConfig build() {
            if (mPassphrase != null && mPmk != null) {
                throw new IllegalStateException(
                        "Can only specify a Passphrase or a PMK - not both!");
            }
            if (mCipherSuite == WIFI_AWARE_CIPHER_SUITE_NCS_SK_128
                    || mCipherSuite == WIFI_AWARE_CIPHER_SUITE_NCS_SK_256) {
                if (TextUtils.isEmpty(mPassphrase) && mPmk == null) {
                    throw new IllegalStateException("Must set either PMK or Passphrase for "
                            + "shared key cipher suite");
                }
                if (mPmkId != null) {
                    throw new IllegalStateException("PMKID should not set for "
                            + "shared key cipher suite");
                }
            } else {
                if (mPmk == null || mPmkId == null) {
                    throw new IllegalStateException("Must set both PMK and PMKID for "
                            + "public key cipher suite");
                }
                if (!TextUtils.isEmpty(mPassphrase)) {
                    throw new IllegalStateException("Passphrase is not support for public "
                            + "key cipher suite");
                }
            }

            return new WifiAwareDataPathSecurityConfig(mCipherSuite, mPmk, mPmkId, mPassphrase);
        }
    }
}
