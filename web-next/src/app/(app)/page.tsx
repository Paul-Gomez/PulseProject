import { HomeView } from "@/components/HomeView";

// Private pages depend on a session that only exists in the browser, so they can't render
// instantly from the server. This opts them out of Next's instant-navigation validation.
export const instant = false;

export default function HomePage() {
  return <HomeView />;
}
