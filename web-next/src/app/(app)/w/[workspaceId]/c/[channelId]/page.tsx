import { ChannelChat } from "@/components/chat/ChannelChat";

export const instant = false;

// key={channelId}: switching channels mounts a fresh chat instead of resetting state inside an effect.
export default async function ChannelPage({ params }: { params: Promise<{ channelId: string }> }) {
  const { channelId } = await params;
  return <ChannelChat key={channelId} channelId={channelId} />;
}
