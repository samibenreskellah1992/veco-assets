package dz.vecopharm.vecoassets.entity;

/**
 * Checkpoint 2 de l'evolution "locaux scannables" (2026-09) : contrat
 * minimal pour tout ce qu'on peut imprimer sur une etiquette
 * (QR/code-barres + texte court) - {@link LabelPdfBuilder} et
 * {@link LabelImageGenerator} ne connaissent que ce contrat, jamais
 * {@link Asset} ou {@link Location} directement, pour que le meme moteur
 * de mise en page serve aux deux sans dupliquer ~150 lignes de logique de
 * positionnement/redimensionnement QR-texte (le contenu du QR/code-barres
 * est le seul identifiant imprime - jamais une donnee personnelle ni une
 * URL, meme regle que l'etiquetage d'immobilisation, voir
 * {@link LabelImageGenerator}).
 */
public interface LabelPrintable {

    /** Contenu encode dans le QR code / code-barres - doit identifier l'objet de facon unique et stable. */
    String getLabelCode();

    /** Texte court affiche sous le logo (designation d'immobilisation, nom de local...). */
    String getLabelDesignation();
}
