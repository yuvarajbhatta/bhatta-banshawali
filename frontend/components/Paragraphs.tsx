// Article bodies are stored as plain text with blank-line paragraph
// breaks (see the seeded content in V6__seed_initial_content.sql); this
// renders them as real <p> elements instead of one pre-wrapped blob.
export function Paragraphs({ text }: { text: string }) {
  const paragraphs = text.split(/\n\s*\n/).map((paragraph) => paragraph.trim()).filter(Boolean);

  return (
    <>
      {paragraphs.map((paragraph, index) => (
        <p key={index}>{paragraph}</p>
      ))}
    </>
  );
}
