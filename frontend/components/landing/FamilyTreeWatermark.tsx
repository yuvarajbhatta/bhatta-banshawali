import styles from "./FamilyTreeWatermark.module.css";

/**
 * Purely decorative, static line-art watermark for the login page's
 * branding panel -- replaces the old animated 3D HeroScene (removed:
 * flagged as distracting movement). A symmetric branching diagram
 * (root -> 2 -> 4 -> 8 nodes) evoking family generations, root anchored
 * to the bottom so it reads sensibly cropped at any panel aspect ratio.
 */
export function FamilyTreeWatermark() {
  return (
    <svg
      className={styles.watermark}
      viewBox="0 0 600 700"
      preserveAspectRatio="xMidYMax slice"
      fill="none"
      aria-hidden="true"
    >
      <g stroke="currentColor" strokeWidth="2" strokeLinecap="round">
        <path d="M300,660 Q300,590 170,520" />
        <path d="M300,660 Q300,590 430,520" />
        <path d="M170,520 Q140,450 90,380" />
        <path d="M170,520 Q210,450 250,380" />
        <path d="M430,520 Q390,450 350,380" />
        <path d="M430,520 Q460,450 510,380" />
        <path d="M90,380 Q70,310 50,240" />
        <path d="M90,380 Q110,315 130,250" />
        <path d="M250,380 Q230,315 210,250" />
        <path d="M250,380 Q270,310 290,240" />
        <path d="M350,380 Q330,310 310,240" />
        <path d="M350,380 Q370,315 390,250" />
        <path d="M510,380 Q490,315 470,250" />
        <path d="M510,380 Q530,310 550,240" />
      </g>
      <g fill="currentColor">
        <circle cx="300" cy="660" r="11" />
        <circle cx="170" cy="520" r="8.5" />
        <circle cx="430" cy="520" r="8.5" />
        <circle cx="90" cy="380" r="7" />
        <circle cx="250" cy="380" r="7" />
        <circle cx="350" cy="380" r="7" />
        <circle cx="510" cy="380" r="7" />
        <circle cx="50" cy="240" r="5.5" />
        <circle cx="130" cy="250" r="5.5" />
        <circle cx="210" cy="250" r="5.5" />
        <circle cx="290" cy="240" r="5.5" />
        <circle cx="310" cy="240" r="5.5" />
        <circle cx="390" cy="250" r="5.5" />
        <circle cx="470" cy="250" r="5.5" />
        <circle cx="550" cy="240" r="5.5" />
      </g>
      <g stroke="currentColor" strokeWidth="1" opacity="0.5">
        <circle cx="300" cy="660" r="19" />
        <circle cx="50" cy="240" r="10" />
        <circle cx="550" cy="240" r="10" />
        <circle cx="300" cy="120" r="3" />
        <circle cx="150" cy="160" r="2.5" />
        <circle cx="450" cy="160" r="2.5" />
      </g>
    </svg>
  );
}
