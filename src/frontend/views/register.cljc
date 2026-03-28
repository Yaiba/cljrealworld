(ns frontend.views.register)

(defn render-ui [username email password errors]
  [:div.min-h-screen.w-full.flex.items-center.justify-center.bg-base-200
   [:div.card.w-96.bg-base-100.shadow-xl
    [:div.card-body
     [:h2.card-title.justify-center.text-2xl "Sign up"]
     [:p.text-center.mb-4
      [:a.link {:on {:click [:app/navigate :page/login]}} "Have an account?"]]
     (when errors
       [:div.alert.alert-error
        [:ul
         (for [[field msgs] errors
               msg msgs]
           [:li {:key (str field msg)} (str (name field) " " msg)])]])
     [:div.form-control.mb-2
      [:input.input.input-bordered
       {:type "text" :placeholder "Username" :value username
        :on {:input [:app/set-username [:event.target/value]]}}]]
     [:div.form-control.mb-2
      [:input.input.input-bordered
       {:type "email" :placeholder "Email" :value email
        :on {:input [:app/set-email [:event.target/value]]}}]]
     [:div.form-control.mb-4
      [:input.input.input-bordered
       {:type "password" :placeholder "Password" :value password
        :on {:input [:app/set-password [:event.target/value]]}}]]
     [:button.btn.btn-primary.w-full
      {:on {:click [:app/register]}}
      "Sign up"]]]])
