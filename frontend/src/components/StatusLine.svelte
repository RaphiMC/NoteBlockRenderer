<script lang="ts">
    import type {Status} from "../lib/app.svelte";

    let {status, inDialog = false}: {status: Status; inDialog?: boolean} = $props();
    const colors = {idle: "text-base-content/60", loading: "text-info", success: "text-success", warning: "text-warning", error: "text-error"};
</script>

<div class={["flex min-h-5 max-w-full min-w-0 items-start gap-2", colors[status.tone], inDialog && "mt-3", !status.message && "invisible"]} role="status" aria-live="polite" aria-atomic="true">
    <span class={["mt-1.5 status shrink-0 bg-current text-inherit", status.tone === "loading" && "animate-pulse"]} aria-hidden="true"></span>
    <span class={["wrap-anywhere", inDialog ? "whitespace-pre-wrap" : "line-clamp-2"]}>{status.message}</span>
</div>
