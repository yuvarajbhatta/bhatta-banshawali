import styles from "./FamilyTreeWatermark.module.css";

/**
 * Purely decorative, static watermark for the login page's branding
 * panel -- the Bhatta Banshawali family portrait, served by
 * BrandAssetController. That panel (.stage) is always dark regardless
 * of site theme, so unlike the site-wide background watermark
 * (globals.css) this doesn't need a light/dark branch: it always gets
 * the "chalk on dark" inverted treatment.
 */
export function FamilyTreeWatermark() {
  return (
    // eslint-disable-next-line @next/next/no-img-element
    <img src="/api/v1/brand/logo" alt="" className={styles.watermark} />
  );
}
