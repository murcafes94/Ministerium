package com.fabri.ministerium;

import android.app.Activity;
import android.view.View;
import android.webkit.WebView;

/** Places the commentary action beside the current canon number only when it exists. */
public final class CanonCommentIcon {
    private CanonCommentIcon() {}

    public static void sync(Activity activity, WebView webView, int canon) {
        if (activity == null || webView == null) return;
        View legacy = activity.findViewById(R.id.btnCanonComments);
        if (legacy != null) legacy.setVisibility(View.GONE);

        boolean available = false;
        try {
            available = CanonCommentaryRepository.find(activity, canon) != null;
        } catch (Exception ignored) {}

        String script = "(function(){"
                + "var article=document.getElementById('canon-" + canon + "');if(!article)return;"
                + "var old=article.querySelectorAll('.comment-link,.canon-comment-icon');"
                + "for(var i=0;i<old.length;i++)old[i].remove();"
                + "var h=article.querySelector('h1');if(!h)return;"
                + "h.style.display='flex';h.style.alignItems='center';h.style.gap='10px';"
                + (available
                ? "var b=document.createElement('button');b.type='button';b.className='canon-comment-icon';"
                + "b.textContent='▣';b.setAttribute('aria-label','Abrir comentario del canon " + canon + "');"
                + "b.setAttribute('title','Abrir comentario');"
                + "b.style.cssText='border:1px solid currentColor;border-radius:8px;background:transparent;"
                + "color:inherit;padding:4px 9px;font:inherit;line-height:1;cursor:pointer';"
                + "b.onclick=function(){location.href='ministerium://canon-comment/" + canon + "';};h.appendChild(b);"
                : "")
                + "})()";
        webView.evaluateJavascript(script, null);
    }
}
