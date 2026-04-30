(ns frontend.views.editor
  (:require [frontend.views.components :as c]))

(defn render-ui [form errors current-user]
  (let [draft-tags (or (:editor/tags form) #{})]
    [:div.min-h-screen.bg-base-200
     (c/navbar current-user)

     [:div.container.mx-auto.px-4.py-10.max-w-2xl
      [:h1.text-3xl.font-bold.mb-6
       (if (:editor/slug form) "Edit Article" "New Article")]

      (c/error-banner errors)

      [:div.card.bg-base-100.shadow-xl
       [:div.card-body
        [:div.form-control.mb-3
         [:input.input.input-bordered.w-full
          {:type "text"
           :placeholder "Article Title"
           :value (or (:editor/title form) "")
           :on {:input [[:editor/set-field :title [:event.target/value]]]}}]]

        [:div.form-control.mb-3
         [:input.input.input-bordered.w-full
          {:type "text"
           :placeholder "What's this article about?"
           :value (or (:editor/description form) "")
           :on {:input [[:editor/set-field :description [:event.target/value]]]}}]]

        [:div.form-control.mb-3
         [:textarea.textarea.textarea-bordered.w-full
          {:rows 8
           :placeholder "Write your article (in markdown)"
           :value (or (:editor/body form) "")
           :on {:input [[:editor/set-field :body [:event.target/value]]]}}]]

        ;; Tag input
        [:div.form-control.mb-4
         [:div.flex.gap-2
          [:input.input.input-bordered.flex-1
           {:type "text"
            :placeholder "Enter tags"
            :value (or (:editor/tag-input form) "")
            :on {:input [[:editor/set-field :tag-input [:event.target/value]]]}}
           [:button.btn.btn-outline
            {:on {:click [[:editor/add-tag]]}}
            "Add Tag"]]
          ;; Tag chips
          (when (seq draft-tags)
            [:div.flex.flex-wrap.gap-2.mt-2
             (for [tag draft-tags]
               [:span.badge.badge-neutral.gap-1
                {:key tag}
                tag
                [:button.btn.btn-xs.btn-ghost.p-0
                 {:on {:click [[:editor/remove-tag tag]]}}
                 "×"]])])]

         [:button.btn.btn-primary.w-full
          {:on {:click [(if (:editor/slug form)
                          [:article/update]
                          [:article/create])]}}
          (if (:editor/slug form) "Update Article" "Publish Article")]]]]]]))
