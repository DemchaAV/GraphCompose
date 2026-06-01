import TopBar from "@/components/TopBar";
import Hero from "@/components/Hero";
import Playground from "@/components/Playground";
import Pipeline from "@/components/Pipeline";
import Gallery from "@/components/Gallery";
import Positioning from "@/components/Positioning";
import Engineering from "@/components/Engineering";
import Cta from "@/components/Cta";
import Footer from "@/components/Footer";

export default function Page() {
  return (
    <>
      <TopBar />
      <main id="top">
        <Hero />          {/* §01 */}
        <Cta />           {/* §02 — install snippet right after the hero so visitors can copy deps without scrolling past 6 sections */}
        <Playground />    {/* §03 */}
        <Pipeline />      {/* §04 */}
        <Gallery />       {/* §05 — unified showcase: 16 CV pairs + 15 letters + 5 templates + 13 features + 3 flagships */}
        <Positioning />   {/* §06 */}
        <Engineering />   {/* §07 */}
      </main>
      <Footer />
    </>
  );
}
