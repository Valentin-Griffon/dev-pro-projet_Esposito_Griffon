package fr.imagedescription.agent;

/**
 * Image transmise à l'agent : ses octets et son type MIME.
 */
public record Image(byte[] octets, String typeMime) {
}
