(ns frontend.views.settings)

(defn render-ui [form errors]
  [:div.min-h-screen.w-full.flex.items-center.justify-center.bg-base-200
   [:div.card.w-full.max-w-lg.bg-base-100.shadow-xl
    [:div.card-body
     [:h2.card-title.justify-center.text-2xl "Your Settings"]
     (when errors
       [:div.alert.alert-error.mb-4
        [:ul
         (for [[field msgs] errors
               msg msgs]
           [:li {:key (str field msg)} (str (name field) " " msg)])]])
     [:div.form-control.mb-2
      [:input.input.input-bordered
       {:type "text" :placeholder "URL of profile picture"
        :value (or (:settings/image form) "")
        :on {:input [[:settings/set-field :image [:event.target/value]]]}}]]
     [:div.form-control.mb-2
      [:input.input.input-bordered
       {:type "text" :placeholder "Username"
        :value (or (:settings/username form) "")
        :on {:input [[:settings/set-field :username [:event.target/value]]]}}]]
     [:div.form-control.mb-2
      [:textarea.textarea.textarea-bordered.w-full
       {:placeholder "Short bio about you"
        :value (or (:settings/bio form) "")
        :on {:input [[:settings/set-field :bio [:event.target/value]]]}}]]
     [:div.form-control.mb-2
      [:input.input.input-bordered
       {:type "email" :placeholder "Email"
        :value (or (:settings/email form) "")
        :on {:input [[:settings/set-field :email [:event.target/value]]]}}]]
     [:div.form-control.mb-4
      [:input.input.input-bordered
       {:type "password" :placeholder "New Password (leave blank to keep current)"
        :value (or (:settings/password form) "")
        :on {:input [[:settings/set-field :password [:event.target/value]]]}}]]
     [:button.btn.btn-primary.w-full.mb-4
      {:on {:click [[:user/update]]}}
      "Update Settings"]
     [:div.divider]
     [:button.btn.btn-error.btn-outline.w-full
      {:on {:click [[:user/logout]]}}
      "Or click here to logout"]]]])
