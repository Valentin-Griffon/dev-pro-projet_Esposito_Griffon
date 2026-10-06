package fr.imagedescription.agent;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class ImageTest {

    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0x10};
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 0};
    private static final byte[] GIF = "GIF89a....".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] WEBP = "RIFF\0\0\0\0WEBPVP8 ".getBytes(StandardCharsets.US_ASCII);

    static Stream<Arguments> formatsValides() {
        return Stream.of(
                Arguments.of("image/jpeg", JPEG),
                Arguments.of("image/png", PNG),
                Arguments.of("image/gif", GIF),
                Arguments.of("image/webp", WEBP));
    }

    @ParameterizedTest
    @MethodSource("formatsValides")
    void accepteUnFormatPrisEnChargeAvecSonEnTeteCorrecte(String typeMime, byte[] octets) {
        Image image = new Image(octets, typeMime);

        assertThat(image.typeMime()).isEqualTo(typeMime);
        assertThat(image.octets()).isEqualTo(octets);
    }

    @Test
    void refuseUnFichierVide() {
        assertThatThrownBy(() -> new Image(new byte[0], "image/png"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("vide");
    }

    @Test
    void refuseUnTypeNonPrisEnCharge() {
        assertThatThrownBy(() -> new Image(PNG, "application/pdf"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("pas pris en charge");
    }

    @Test
    void refuseUnTypeAbsent() {
        assertThatThrownBy(() -> new Image(PNG, null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void refuseUnContenuQuiNAPasLeTypeAnnonce() {
        assertThatThrownBy(() -> new Image(PNG, "image/jpeg"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ne correspond pas");
    }

    @Test
    void refuseUnWebpSansLaMarqueWEBP() {
        byte[] riffWave = "RIFF\0\0\0\0WAVEfmt ".getBytes(StandardCharsets.US_ASCII);

        assertThatThrownBy(() -> new Image(riffWave, "image/webp"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void refuseDesOctetsNulsEtLesOctetsTropCourts() {
        assertThatNullPointerException().isThrownBy(() -> new Image(null, "image/png"));
        assertThatThrownBy(() -> new Image(new byte[]{(byte) 0x89, 'P'}, "image/png"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void copieDefensiveDesOctetsALaConstructionEtALaLecture() {
        byte[] source = PNG.clone();
        Image image = new Image(source, "image/png");

        source[0] = 0;
        assertThat(image.octets()[0]).isEqualTo((byte) 0x89);

        image.octets()[0] = 0;
        assertThat(image.octets()[0]).isEqualTo((byte) 0x89);
    }
}
