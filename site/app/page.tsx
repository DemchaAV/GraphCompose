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
        <Hero />
        <Playground />
        <Pipeline />
        <Gallery />
        <Positioning />
        <Engineering />
        <Cta />
      </main>
      <Footer />
    </>
  );
}
